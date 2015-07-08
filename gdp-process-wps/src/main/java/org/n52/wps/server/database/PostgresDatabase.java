package org.n52.wps.server.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.naming.NamingException;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.n52.wps.ServerDocument;
import org.n52.wps.commons.PropertyUtil;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.database.connection.ConnectionHandler;
import org.n52.wps.server.database.connection.DefaultConnectionHandler;
import org.n52.wps.server.database.connection.JNDIConnectionHandler;
import org.n52.wps.server.database.domain.WpsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.sql.Savepoint;
import java.util.HashSet;
import java.util.Set;

import org.n52.wps.server.database.domain.WpsInput;
import org.n52.wps.server.database.domain.WpsOutput;
import org.n52.wps.server.database.domain.WpsOutputDefinition;
import org.n52.wps.server.database.domain.WpsResponse;
import org.n52.wps.server.database.domain.WpsStatus;

/**
 *
 * @author isuftin
 */
public class PostgresDatabase extends AbstractDatabase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDatabase.class);
	
	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final String KEY_DATABASE_ROOT = "org.n52.wps.server.database";
	private static final String KEY_DATABASE_PATH = "path";
	private static final String KEY_DATABASE_WIPE_ENABLED = "wipe.enabled";
	private static final String KEY_DATABASE_WIPE_PERIOD = "wipe.period";
	private static final String KEY_DATABASE_WIPE_THRESHOLD = "wipe.threshold";
	private static final boolean DEFAULT_DATABASE_WIPE_ENABLED = true;
	private static final long DEFAULT_DATABASE_WIPE_PERIOD = 1000 * 60 * 60; // default to running once an hour
	private static final long DEFAULT_DATABASE_WIPE_THRESHOLD = 1000 * 60 * 60 * 24 * 7; // default to wipe things over a week old

	private static final String FILE_URI_PREFIX = "file://";
	private static final String SUFFIX_GZIP = "gz";
	private static final String DEFAULT_BASE_DIRECTORY
	= Joiner.on(File.separator).join(System.getProperty("java.io.tmpdir", "."), "Database", "Results");
	private static final ServerDocument.Server server = WPSConfig.getInstance().getWPSConfig().getServer();
	private static final String baseResultURL = String.format("%s://%s:%s/%s/RetrieveResultServlet?id=",
			server.getProtocol(), server.getHostname(), server.getHostport(), server.getWebappPath());

	private static final int SELECTION_STRING_REQUEST_ID_PARAM_INDEX = 1;
	private static final int SELECTION_STRING_RESPONSE_COLUMN_INDEX = 1;
	private static final int SELECTION_STRING_RESPONSE_MIMETYPE_COLUMN_INDEX = 2;

	private static String connectionURL;
	private static Path BASE_DIRECTORY;
	private static PostgresDatabase instance;
	private static ConnectionHandler connectionHandler;
	private final static  boolean SAVE_RESULTS_TO_DB = Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"));
	protected final Object storeResponseLock = new Object();

	private static Timer wipeTimer;
	private final String DATABASE_NAME;

	// SQL DATABASE CREATION
	private static final String REQUEST_TABLE_NAME = "request";
	private static final String INPUT_TABLE_NAME = "input";
	private static final String OUTPUT_DEF_TABLE_NAME = "output_definition";
	private static final String OUTPUT_TABLE_NAME = "output";
	private static final String RESPONSE_TABLE_NAME = "response";
	
	
//	private static final String CREATE_RESULTS_TABLE_PSQL
//	= "CREATE TABLE " + RESULT_TABLE_NAME +" ("
//			+ "REQUEST_ID VARCHAR(100) NOT NULL PRIMARY KEY, "
//			+ "REQUEST_DATE TIMESTAMP, "
//			+ "RESPONSE_TYPE VARCHAR(100), "
//			+ "RESPONSE TEXT, "
//			+ "RESPONSE_MIMETYPE VARCHAR(100))";
	
	private static final String CREATE_REQUEST_TABLE_PSQL
		= "CREATE TABLE " + REQUEST_TABLE_NAME + " ("
			+ "REQUEST_ID VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ "WPS_ALGORITHM_IDENTIFIER VARCHAR(200),"
			+ "REQUEST_XML TEXT)";
	
	private static final String CREATE_INPUT_TABLE_PSQL
		= "CREATE TABLE " + INPUT_TABLE_NAME + " ("
			+ "ID VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ "REQUEST_ID VARCHAR(100),"
			+ "INPUT_IDENTIFIER VARCHAR(200),"
			+ "INPUT_VALUE VARCHAR(500))";
			
	private static final String CREATE_OUTPUT_DEF_TABLE_PSQL
		= "CREATE TABLE " + OUTPUT_DEF_TABLE_NAME + " ("
			+ "ID VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ "REQUEST_ID VARCHAR(100),"
			+ "MIME_TYPE VARCHAR(100),"
			+ "OUTPUT_IDENTIFIER VARCHAR(200))";

	private static final String CREATE_RESPONSE_TABLE_PSQL
		= "CREATE TABLE " + RESPONSE_TABLE_NAME + " ("
			+ "ID VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ "REQUEST_ID VARCHAR(100),"
			+ "WPS_ALGORITHM_IDENTIFIER VARCHAR(200),"
			+ "STATUS VARCHAR(50),"
			+ "PERCENT_COMPLETE INTEGER,"
			+ "CREATION_TIME TIMESTAMP with time zone,"
			+ "START_TIME TIMESTAMP with time zone,"
			+ "END_TIME TIMESTAMP with time zone)";
	
	/*
	 * OUTPUT_ID is request_id concattenated with wps output identifier (ex. OUTPUT)
	 * Not to be confused with OUTPUT_IDENTIFER which is just the identifier
	 */
	private static final String CREATE_OUTPUT_TABLE_PSQL
		= "CREATE TABLE " + OUTPUT_TABLE_NAME + " ("
			+ "ID VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ "OUTPUT_ID VARCHAR(100),"
			+ "RESPONSE_ID VARCHAR(100),"
			+ "INLINE_RESPONSE TEXT,"
			+ "MIME_TYPE VARCHAR(100),"
			+ "RESPONSE_LENGTH BIGINT,"
			+ "LOCATION VARCHAR(200))";

	private static final ImmutableMap<String, String> CREATE_TABLE_MAP = ImmutableMap.<String, String>builder()
//		.put(RESULT_TABLE_NAME, CREATE_RESULTS_TABLE_PSQL)
		.put(REQUEST_TABLE_NAME, CREATE_REQUEST_TABLE_PSQL)
		.put(INPUT_TABLE_NAME, CREATE_INPUT_TABLE_PSQL)
		.put(OUTPUT_DEF_TABLE_NAME, CREATE_OUTPUT_DEF_TABLE_PSQL)
		.put(OUTPUT_TABLE_NAME, CREATE_OUTPUT_TABLE_PSQL)
		.put(RESPONSE_TABLE_NAME, CREATE_RESPONSE_TABLE_PSQL).build();
	
	// SQL STATEMENTS
	private static final String INSERT_REQUEST_STATEMENT = "INSERT INTO " + REQUEST_TABLE_NAME + " VALUES(?, ?, ?)";
	private static final String INSERT_INPUT_STATEMENT = "INSERT INTO " + INPUT_TABLE_NAME + " VALUES (?, ?, ?, ?)";
	private static final String INSERT_OUTPUT_DEF_STATEMENT = "INSERT INTO " + OUTPUT_DEF_TABLE_NAME + " VALUES (?, ?, ?, ?)";
	private static final String INSERT_RESPONSE_STATEMENT = "INSERT INTO " + RESPONSE_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_RESPONSE_STATEMENT = "UPDATE " + RESPONSE_TABLE_NAME + " SET STATUS=?,PERCENT_COMPLETE=?,END_TIME=? WHERE REQUEST_ID = ?";
	private static final String INSERT_OUTPUT_STATEMENT = "INSERT INTO " + OUTPUT_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?)";
	private static final String SELECT_RESPONSE_STATEMENT = "SELECT * FROM " + RESPONSE_TABLE_NAME + " WHERE REQUEST_ID = ?";
	private static final String SELECT_OUTPUT_STATEMENT = "SELECT * FROM " + OUTPUT_TABLE_NAME + " WHERE OUTPUT_ID = ?";
	
	private PostgresDatabase() {
		PropertyUtil propertyUtil = new PropertyUtil(server.getDatabase().getPropertyArray(), KEY_DATABASE_ROOT);
		String baseDirectoryPath = propertyUtil.extractString(KEY_DATABASE_PATH, DEFAULT_BASE_DIRECTORY);
		String dbName = getDatabaseProperties(PROPERTY_NAME_DATABASE_NAME);
		DATABASE_NAME = (StringUtils.isBlank(dbName)) ? "wps" : dbName;
		try {
			Class.forName("org.postgresql.Driver");
			initializeBaseDirectory(baseDirectoryPath);
			initializeConnectionHandler();
			initializeTables();
			initializeDatabaseWiper(propertyUtil);
		} catch (IOException | SQLException | NamingException ex) {
			LOGGER.error("Error creating PostgresDatabase", ex);
			throw new RuntimeException("Error creating PostgresDatabase", ex);
		} catch (ClassNotFoundException ex) {
			LOGGER.error("The database class could not be loaded.", ex);
			throw new UnsupportedDatabaseException("The database class could not be loaded.", ex);
		}
	}

	private void initializeBaseDirectory(final String baseDirectoryPath) throws IOException {
		BASE_DIRECTORY = Paths.get(baseDirectoryPath);
		LOGGER.info("Using \"{}\" as base directory for results database", baseDirectoryPath);
		Files.createDirectories(BASE_DIRECTORY);
	}

	private void initializeDatabaseWiper(PropertyUtil propertyUtil) {
		if (propertyUtil.extractBoolean(KEY_DATABASE_WIPE_ENABLED, DEFAULT_DATABASE_WIPE_ENABLED)) {
			long periodMillis = propertyUtil.extractPeriodAsMillis(KEY_DATABASE_WIPE_PERIOD, DEFAULT_DATABASE_WIPE_PERIOD);
			long thresholdMillis = propertyUtil.extractPeriodAsMillis(KEY_DATABASE_WIPE_THRESHOLD, DEFAULT_DATABASE_WIPE_THRESHOLD);
			wipeTimer = new Timer(PostgresDatabase.class.getSimpleName() + " Postgres Wiper", true);
			wipeTimer.scheduleAtFixedRate(new PostgresDatabase.WipeTimerTask(thresholdMillis), 15000, periodMillis);
			LOGGER.info("Started {} Postgres wiper timer; period {} ms, threshold {} ms",
					new Object[]{DATABASE_NAME, periodMillis, thresholdMillis});
		} else {
			wipeTimer = null;
		}
	}

	private void initializeConnectionHandler() throws SQLException, NamingException {
		String jndiName = getDatabaseProperties("jndiName");
		if (null != jndiName) {
			connectionHandler = new JNDIConnectionHandler(jndiName);
		} else {
			connectionURL = "jdbc:postgresql:" + getDatabasePath() + "/" + DATABASE_NAME;
			LOGGER.debug("Database connection URL is: " + connectionURL);
			String username = getDatabaseProperties("username");
			String password = getDatabaseProperties("password");
			Properties props = new Properties();
			props.setProperty("create", "true");
			props.setProperty("user", username);
			props.setProperty("password", password);
			connectionHandler = new DefaultConnectionHandler(connectionURL, props);
		}
	}

	private void initializeTables() throws SQLException {
		try (Connection connection = connectionHandler.getConnection();
				ResultSet rs = getTables(connection)) {
			Set<String> tableNames = new HashSet<>();
			while (rs.next()) {
				tableNames.add(rs.getString("table_name"));
			}
			for (String expectedTableName : CREATE_TABLE_MAP.keySet()) {
				if (!tableNames.contains(expectedTableName)) {
					try (Statement st = connection.createStatement()) {
						LOGGER.debug("Table: " + expectedTableName + " does not yet exist, creating it.");
						st.executeUpdate(CREATE_TABLE_MAP.get(expectedTableName));
					}
				}
			}
		}
	}

	public static synchronized PostgresDatabase getInstance() {
		if (instance == null) {
			instance = new PostgresDatabase();
		}
		return instance;
	}

	@Override
	public String getConnectionURL() {
		return connectionURL;
	}

	@Override
	public Connection getConnection() {
		try {
			return connectionHandler.getConnection();
		} catch (SQLException ex) {
			throw new RuntimeException("Unable to obtain connection to database!", ex);
		}
	}

	private ResultSet getTables(Connection connection) throws SQLException {
		return connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
	}

	@Override
	public String generateRetrieveResultURL(String id) {
		return baseResultURL + id;
	}

	@Override
	public void insertRequest(String id, InputStream inputStream, boolean xml) {
		if (xml) {
			WpsRequest wpsReq = new WpsRequest(id, inputStream);
			insertWpsRequest(wpsReq);
		} else{
			//TODO eventually we may need to support KVP (non xml) execution
			String msg = "PostgreseDatabase does not support persisting non xml";
			LOGGER.error(msg);
			throw new UnsupportedOperationException(msg);
		}
	}

	private void insertWpsRequest(WpsRequest wpsReq) {
		
		Savepoint transaction = null;
		try (Connection connection = getConnection();
				PreparedStatement insertRequestStatement = connection.prepareStatement(INSERT_REQUEST_STATEMENT);
				PreparedStatement insertInputStatement = connection.prepareStatement(INSERT_INPUT_STATEMENT);
				PreparedStatement insertOutputDefStatement = connection.prepareStatement(INSERT_OUTPUT_DEF_STATEMENT)) {
			
			connection.setAutoCommit(false);
			transaction = connection.setSavepoint();
			
			insertRequest(insertRequestStatement, wpsReq);
			insertInputs(wpsReq.getWpsInputs(), insertInputStatement);
			insertOutputDefs(wpsReq.getWpsRequestedOutputs(), insertOutputDefStatement);
			
			connection.commit();
		} catch (Exception e) {
			String msg = "Failed to insert request into database";
			try {
				if (connection != null) {
					connection.rollback(transaction);
				}
			} catch (Exception e2) {
				LOGGER.error("Unable to rollback changes", e2);
			}
			LOGGER.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	private void insertRequest(PreparedStatement insertRequestStatement, WpsRequest wpsReq) throws SQLException {
		insertRequestStatement.setString(1, wpsReq.getId());
		insertRequestStatement.setString(2, wpsReq.getWpsAlgoIdentifer());
		insertRequestStatement.setString(3, wpsReq.getExecuteDoc().xmlText());
		insertRequestStatement.execute();
	}
	
	private void insertInputs(List<WpsInput> inputs, PreparedStatement preparedStatement) throws SQLException {
		for (WpsInput input : inputs) {
			preparedStatement.setString(1, input.getId());
			preparedStatement.setString(2, input.getWpsRequestId());
			preparedStatement.setString(3, input.getInputId());
			preparedStatement.setString(4, input.getValue());
			preparedStatement.addBatch();
		}
		preparedStatement.executeBatch();
	}
	
	private void insertOutputDefs(List<WpsOutputDefinition> outputs, PreparedStatement preparedStatement) throws SQLException {
		for (WpsOutputDefinition output : outputs) {
			preparedStatement.setString(1, output.getId());
			preparedStatement.setString(2, output.getWpsRequestId());
			preparedStatement.setString(3, output.getOutputIdentifier());
			preparedStatement.setString(3, output.getOutputIdentifier());
			preparedStatement.addBatch();
		}
		preparedStatement.executeBatch();
	}

	@Override
	public String insertResponse(String id, InputStream inputStream) {
		WpsResponse wpsResponse = new WpsResponse(id, inputStream);
		wpsResponse.setStartTime(new DateTime());
		insertWpsResponse(wpsResponse);
		return generateRetrieveResultURL(id);
	}
	

	private void insertWpsResponse(WpsResponse wpsResp) {
		Savepoint transaction = null;
		try (Connection connection = getConnection();
				PreparedStatement insertResponseStatement = connection.prepareStatement(INSERT_RESPONSE_STATEMENT)) {
			
			connection.setAutoCommit(false);
			transaction = connection.setSavepoint();
			
			insertResponseToDb(insertResponseStatement, wpsResp);
			
			connection.commit();
		} catch (Exception e) {
			String msg = "Failed to insert request into database";
			try {
				if (connection != null) {
					connection.rollback(transaction);
				}
			} catch (Exception e2) {
				LOGGER.error("Unable to rollback chnages", e2);
			}
			LOGGER.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	private void insertResponseToDb(PreparedStatement insertResponseStatement, WpsResponse wpsResp) throws SQLException {	
		insertResponseStatement.setString(1, wpsResp.getId());
		insertResponseStatement.setString(2, wpsResp.getWpsRequestId());
		insertResponseStatement.setString(3, wpsResp.getWpsAlgoIdentifer());
		insertResponseStatement.setString(4, wpsResp.getStatus().toString());
		insertResponseStatement.setInt(5, wpsResp.getPercentComplete() == null ? 0 : wpsResp.getPercentComplete());
		insertResponseStatement.setDate(6, toSQLDate(wpsResp.getCreationTime()));
		insertResponseStatement.setDate(7, toSQLDate(wpsResp.getStartTime()));
		insertResponseStatement.setDate(8, toSQLDate(wpsResp.getEndTime()));
		insertResponseStatement.execute();
	}
	

	private void persistOutput(WpsOutput output, PreparedStatement insertOutputStatement) throws SQLException {
		if (output != null) {
			insertOutputStatement.setString(1, output.getId());
			insertOutputStatement.setString(2, output.getWpsResponseId());
			insertOutputStatement.setString(3, output.getOutputId());
			//NOTE either content or location is populated, based on type of output (inline or not)
			String content = output.getContent();
			insertOutputStatement.setString(4, content);
			insertOutputStatement.setString(5, output.getMimeType());
			insertOutputStatement.setLong(6, output.getResponseLength());
			insertOutputStatement.setString(7, output.getLocation());
			insertOutputStatement.executeUpdate();
		}
	}
	
	private Date toSQLDate(DateTime dateTime) {
		return (dateTime == null) ? null : new Date(dateTime.getMillis());
	}
	
	@Override
	public synchronized String storeComplexValue(String requestid, String outputIdentifier, InputStream stream, String type, String mimeType) {
		String wpsResponseId = readWpsResponseFromDB(requestid).getId();
		// For now id will just be requestId + wps identifier which is unique as long as requests can only run once
		String outputId = requestid + outputIdentifier;
		WpsOutput output = new WpsOutput(wpsResponseId, outputId, mimeType);
		if (SAVE_RESULTS_TO_DB) {
			output.setInline(stream);
		} else{
			try {
				// The result contents won't be saved to the database, only a pointer to the file system. I am therefore
				// going to GZip the data to save space
				String referenceLocation = writeInputStreamToDisk(requestid, stream, true);
				output.setLocation(referenceLocation);
			} catch (IOException ex) {
				LOGGER.error("Failed to write output data to disk", ex);
			}
		}
		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(INSERT_OUTPUT_STATEMENT)) {
			persistOutput(output, statement);
		} catch (Exception e) {
			throw new RuntimeException("issue writing output", e);
		}
		return generateRetrieveResultURL(outputId);
	}
	
	/**
	 * Writes an input stream to disk
	 * @param filename base filename
	 * @param data String of data to write to disk, compressed using gzip
	 * @param compress true to GZip results
	 * @return String of the file URI pointing where the data was written
	 * @throws Exception
	 */
	private String writeInputStreamToDisk(String filename, InputStream data, boolean compress) throws IOException {
		Path filePath = BASE_DIRECTORY.resolve(Joiner.on(".").join(filename, SUFFIX_GZIP));
		Files.deleteIfExists(filePath);
		Path createdFilePath = Files.createFile(filePath);

		OutputStream os = new FileOutputStream(createdFilePath.toFile());

		if (compress) {
			os = new GZIPOutputStream(os);
		}

		IOUtils.copyLarge(data, os);
		IOUtils.closeQuietly(os);
		return createdFilePath.toUri().toString().replaceFirst(FILE_URI_PREFIX, "");
	}
	
	private WpsResponse readWpsResponseFromDB(String requestid) {
		WpsResponse ret = null;
		try (Connection connection = getConnection();
				PreparedStatement selectRequestStatement = connection.prepareStatement(SELECT_RESPONSE_STATEMENT)) {
			
			selectRequestStatement.setString(1, requestid);
			
			ResultSet rs = selectRequestStatement.executeQuery();
			
			if (rs != null && rs.next()) {
				ret = constructResponseFromRs(rs);
			}
		} catch (Exception e) {
			String msg = "Failed to select request from database";
			LOGGER.error(msg, e);
			throw new RuntimeException(msg, e);
		}
		return ret;
	}
	
	private WpsResponse constructResponseFromRs(ResultSet rs) {
		WpsResponse ret = null;
		if (rs != null) {
			try {
				ret = new WpsResponse(rs.getString("ID"), rs.getString("REQUEST_ID"),
						rs.getString("WPS_ALGORITHM_IDENTIFIER"), WpsStatus.valueOf(rs.getString("STATUS")),
						rs.getInt("PERCENT_COMPLETE"), new DateTime(rs.getTimestamp("CREATION_TIME")));
			} catch (SQLException e) {
				String msg = "Failed to build response from database";
				LOGGER.error(msg, e);
				throw new RuntimeException(msg, e);
			}
		}
		return ret;
	}

	private WpsOutput readOutputFromDB(String outputId) {
		WpsOutput ret = null;
		
		try (Connection connection = getConnection();
				PreparedStatement selectRequestStatement = connection.prepareStatement(SELECT_OUTPUT_STATEMENT);) {
			selectRequestStatement.setString(1, outputId);
			
			ResultSet rs = selectRequestStatement.executeQuery();
			
			if (rs != null && rs.next()) {
				ret = constructOutputFromRs(rs);
			}
		} catch (Exception e) {
			String msg = "Failed to select request from database";
			LOGGER.error(msg, e);
			throw new RuntimeException(msg, e);
		}
		return ret;
	}

	private WpsOutput constructOutputFromRs(ResultSet rs) {
		WpsOutput ret = null;
		if (rs != null) {
			try {
				ret = new WpsOutput(rs.getString("ID"), rs.getString("OUTPUT_ID"),
						rs.getString("RESPONSE_ID"), rs.getString("INLINE_RESPONSE"),
						rs.getInt("MIME_TYPE"), rs.getLong("RESPONSE_LENGTH"), rs.getString("LOCATION"));
			} catch (SQLException e) {
				String msg = "Failed to build output from database";
				LOGGER.error(msg, e);
				throw new RuntimeException(msg, e);
			}
		}
		return ret;
	}

	@Override
	public long getContentLengthForStoreResponse(String id) {
		long contentLength = -1;
		WpsResponse response = readWpsResponseFromDB(id);
		if (response == null) {
			WpsOutput output = readOutputFromDB(id);
			contentLength = output.getResponseLength();
		}
		return contentLength;
	}

	"ID VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ "REQUEST_ID VARCHAR(100),"
			+ "WPS_ALGORITHM_IDENTIFIER VARCHAR(200),"
			+ "STATUS VARCHAR(50),"
			+ "PERCENT_COMPLETE INTEGER,"
			+ "CREATION_TIME TIMESTAMP with time zone,"
			+ "START_TIME TIMESTAMP with time zone,"
			+ "END_TIME TIMESTAMP with time zone)";
	/**
	 * This is called when response already exists and just needs status or other updates set
	 * @param id
	 * @param stream 
	 */
	@Override
	public void updateResponse(String id, InputStream stream) {
		WpsResponse wpsResponse = new WpsResponse(id, stream);
		if (wpsResponse.getStatus() == WpsStatus.SUCCEEDED || wpsResponse.getStatus() == WpsStatus.FAILED) {
			wpsResponse.setEndTime(new DateTime());
		}
				
		try (Connection connection = getConnection();
			PreparedStatement updateStatement = connection.prepareStatement(UPDATE_RESPONSE_STATEMENT)) {
			
			updateStatement.setString(1, wpsResponse.getStatus().toString());
			updateStatement.setInt(2, wpsResponse.getPercentComplete());
			updateStatement.setDate(3, toSQLDate(wpsResponse.getEndTime()));
			updateStatement.setString(4, id);
			
			updateStatement.executeUpdate();

			LOGGER.debug("Updated response into database with id of:" + id);
		} catch (SQLException | IOException ex) {
			LOGGER.error(MessageFormat.format("Failed to update data in database with  id of:{0}", id), ex);
		}
	}

	@Override
	public InputStream lookupResponse(String id) {
		
		InputStream result = null;
		synchronized (storeResponseLock) {
			if (StringUtils.isNotBlank(id)) {
				
				//first select to see if what we are looking up is in the Response Table
				WpsResponse responseFromDb = readWpsResponseFromDB(id);
				//next select to see if what we are looking up is in the output Table
				WpsOutput outputFromDb = readOutputFromDB(id);
				
				result = responseFromDb.toXML(); //serialize to XML 
				
				try (Connection connection = getConnection();
					PreparedStatement selectStatement = connection.prepareStatement(selectionString)) {
					selectStatement.setString(SELECTION_STRING_REQUEST_ID_PARAM_INDEX, id);

					try (ResultSet rs = selectStatement.executeQuery()) {
						if (null == rs || !rs.next()) {
							LOGGER.warn("No response found for request id " + id);
						} else {
							result = rs.getAsciiStream(SELECTION_STRING_RESPONSE_COLUMN_INDEX);
							// Copy the file to disk and create an inputstream from that because once I leave
							// this function, result will not be accessible since the connection to the database 
							// will be broken. I eat a bit of overhead this way, but afaik, it's the best solution
							File tempFile = Files.createTempFile("GDP-SAFE-TO-DELETE-" + id, null).toFile();

							// Best effort, even though SelfCleaningFileInputStream should delete it
							tempFile.deleteOnExit();

							// Copy the ASCII stream to file
							IOUtils.copyLarge(result, new FileOutputStream(tempFile));
							IOUtils.closeQuietly(result);

							// Create an InputStream (of the self-cleaning type) from this File and pass that on
							result = new SelfCleaningFileInputStream(tempFile);
						}
					} catch (IOException ex) {
						LOGGER.error("Could not look up response in database", ex);
					}
				} catch (SQLException ex) {
					LOGGER.error("Could not look up response in database", ex);
				}

				if (null != result) {
					if (!SAVE_RESULTS_TO_DB) {
						try {
							String outputFileLocation = IOUtils.toString(result);
							LOGGER.debug("ID {} is output and saved to disk instead of database. Path = " + outputFileLocation);
							if (Files.exists(Paths.get(outputFileLocation))) {
								result = new GZIPInputStream(new FileInputStream(outputFileLocation));
							} else {
								LOGGER.warn("Response not found on disk for id " + id + " at " + outputFileLocation);
							}
						} catch (FileNotFoundException ex) {
							LOGGER.warn("Response not found on disk for id " + id, ex);
						} catch (IOException ex) {
							LOGGER.warn("Error processing response for id " + id, ex);
						}
					}
				} else {
					LOGGER.warn("response found but returned null");
				}

			} else {
				LOGGER.warn("tried to look up response for null id, returned null");
			}
		}
		return result;
	}

	aa
//	TODO switch *all* selectionString to something else (it goes against the Results table which is now not in use for postgres)
	
	@Override
	public String getMimeTypeForStoreResponse(String id) {
		// TODO check if this is status or output
		String mimeType = null;
		try (Connection connection = getConnection(); PreparedStatement selectStatement = connection.prepareStatement(selectionString)) {
			selectStatement.setString(SELECTION_STRING_REQUEST_ID_PARAM_INDEX, id);
			try (ResultSet rs = selectStatement.executeQuery()) {
				if (null == rs || !rs.next()) {
					LOGGER.warn("No response found for request id " + id);
				} else {
					mimeType = rs.getString(SELECTION_STRING_RESPONSE_MIMETYPE_COLUMN_INDEX);
				}
			}
		} catch (SQLException ex) {
			LOGGER.error("Could not look up response in database", ex);
		}
		return mimeType;
	}

	@Override
	public File lookupResponseAsFile(String id) {
		throw new UnsupportedOperationException("This is only supported in FlatFileDatabase");
	}

	private class WipeTimerTask extends TimerTask {

		private final long thresholdMillis;
		private static final String DELETE_STATEMENT = "DELETE FROM RESULTS WHERE RESULTS.REQUEST_ID = ANY ( ? ) AND RESULTS.REQUESTS_ID NOT LIKE 'REQ_%';";aa //TODO wipe from response & output table?
		private static final int DELETE_STATEMENT_LIST_PARAM_INDEX = 1;
		private static final String LOOKUP_STATEMENT = "SELECT * FROM "
				+ "(SELECT REQUEST_ID, EXTRACT(EPOCH FROM REQUEST_DATE) * 1000 AS TIMESTAMP FROM RESULTS) items WHERE TIMESTAMP < ?";
		private static final int LOOKUP_STATEMENT_TIMESTAMP_PARAM_INDEX = 1;
		private static final int LOOKUP_STATEMENT_REQUEST_ID_COLUMN_INDEX = 1;
		private final String databaseName = getDatabaseName();

		WipeTimerTask(long thresholdMillis) {
			this.thresholdMillis = thresholdMillis;
		}

		@Override
		public void run() {
			LOGGER.info(databaseName + " Postgres wiper, checking for records older than {} ms", thresholdMillis);
			try {

				int deletedRecordsCount = wipe();
				if (deletedRecordsCount > 0) {
					LOGGER.info(databaseName + " Postgres wiper, cleaned {} records from database", deletedRecordsCount);
				} else {
					LOGGER.debug(databaseName + " Postgres wiper, cleaned {} records from database", deletedRecordsCount);
				}
			} catch (SQLException | IOException ex) {
				LOGGER.warn(databaseName + " Postgres wiper, failed to deleted old records", ex);
			}
		}

		private int wipe() throws SQLException, IOException {
			LOGGER.debug(databaseName + " Postgres wiper, checking for records older than {} ms", thresholdMillis);
			int deletedRecordsCount = 0;
			List<String> oldRecords = findOldRecords();
			if (!SAVE_RESULTS_TO_DB) {
				for (String recordId : oldRecords) {
					if (recordId.toLowerCase(Locale.US).contains("output")) {
						Files.deleteIfExists(Paths.get(BASE_DIRECTORY.toString(), recordId));
					}
				}
			}
			if (!oldRecords.isEmpty()) {
				deletedRecordsCount = deleteRecords(oldRecords);
			}
			return deletedRecordsCount;
		}

		private int deleteRecords(List<String> recordIds) throws SQLException {
			int deletedRecordsCount;
			try (Connection connection = connectionHandler.getConnection(); PreparedStatement deleteStatement = connection.prepareStatement(DELETE_STATEMENT)) {
				deleteStatement.setArray(DELETE_STATEMENT_LIST_PARAM_INDEX, connection.createArrayOf("varchar", recordIds.toArray()));
				deletedRecordsCount = deleteStatement.executeUpdate();
			}
			return deletedRecordsCount;
		}

		private List<String> findOldRecords() throws SQLException {
			List<String> matchingRecords = new ArrayList<>();
			try (Connection connection = connectionHandler.getConnection(); PreparedStatement lookupStatement = connection.prepareStatement(LOOKUP_STATEMENT)) {
				long ageMillis = System.currentTimeMillis() - thresholdMillis;
				lookupStatement.setLong(LOOKUP_STATEMENT_TIMESTAMP_PARAM_INDEX, ageMillis);
				try (ResultSet rs = lookupStatement.executeQuery()) {
					while (rs.next()) {
						matchingRecords.add(rs.getString(LOOKUP_STATEMENT_REQUEST_ID_COLUMN_INDEX));
					}
				}
			}
			return matchingRecords;
		}
	}
}

