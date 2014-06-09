# wps.des: id = gridded_daily, title = A generalized daily climate statistics algorithm, abstract = TBD; 
# wps.in: start, string, Start Year, Start Year (ie. 1950);
# wps.in: end, string, End Year, End Year (ie. 2000);
# wps.in: bbox_in, string, BBOX, Format, comma seperated min lat/lon max lat/lon;
# wps.in: days_tmax_abv_thresh, string, Days with tmax above threshold, comma seperated list of thresholds in degrees C, value = "";
# wps.in: days_tmin_blw_thresh, string, Days with tmin below threshold, comma seperated list of thresholds in degrees C, value = "";
# wps.in: days_prcp_abv_thresh, string, Days with prcp above threshold, comma seperated list of thresholds in mm, value = "";
# wps.in: longest_run_tmax_abv_thresh, string, Longest run with tmax above threshold, comma seperated list of thresholds in degrees C, value = "";
# wps.in: longest_run_prcp_blw_thresh, string, Longest run with tmin below threshold, comma seperated list of thresholds in mm, value = "";
# wps.in: growing_degree_day_thresh, string, Growing degree days, comma seperated list of thresholds in degrees C, value = "";
# wps.in: heating_degree_day_thresh, string, Heating degree days, comma seperated list of thresholds in degrees C, value = "";
# wps.in: cooling_degree_day_thresh, string, Cooling degree days, comma seperated list of thresholds in degrees C, value = "";
# wps.in: growing_season_lngth_thresh, string, Growing season length, comma seperated list of thresholds in degrees C, value = "";
# wps.in: OPeNDAP_URI, string, OPeNDAP URI, An OPeNDAP (dods) url for the climate dataset of interest.;
# wps.in: tmax_var, string, Tmax Variable, The variable from the OPeNDAP dataset to use as tmax.;
# wps.in: tmin_var, string, Tmin Variable, The variable from the OPeNDAP dataset to use as tmin.;
# wps.in: tave_var, string, Tave Variable, The variable from the OPeNDAP dataset to use as tave can be "NULL".;
# wps.in: prcp_var, string, Prcp Variable, The variable from the OPeNDAP dataset to use as prcp.;
library("dapClimates")
library("climates") # Because climates uses depends on stuff, this is needed as well as the dapClimates load.

t_names<-c("days_tmax_abv_thresh",
  "days_tmin_blw_thresh",
  "days_prcp_abv_thresh",
  "longest_run_tmax_abv_thresh",
  "longest_run_prcp_blw_thresh",
  "growing_degree_day_thresh",
  "heating_degree_day_thresh",
  "cooling_degree_day_thresh",
  "growing_season_lngth_thresh")

thresholds<-list()
for(t_name in t_names) {
  tn<-get(t_name)
  if(tn!="") thresholds[[t_name]] <- as.double(read.csv(header=F,colClasses=c("character"),text=tn))
}
bbox_in <- as.double(read.csv(header=F,colClasses=c("character"),text=bbox_in))
if(tave_var=="NULL") tave_var<-NULL
fileNames<-dap_daily_stats(start,end,bbox_in,thresholds,OPeNDAP_URI,tmax_var,tmin_var,tave_var,prcp_var)
name<-'dailyInd.zip'
dailyInd_zip<-zip(name,fileNames)
#wps.out: name, zip, bioclim_zip, A zip of the resulting bioclim geotiffs.;
