/*
 * --------------------------------------------------------------------
 * jQuery-Plugin - $.download - allows for simple get/post requests for files
 * by Scott Jehl, scott@filamentgroup.com
 * http://www.filamentgroup.com
 * reference article: http://www.filamentgroup.com/lab/jquery_plugin_for_requesting_ajax_like_file_downloads/
 * Copyright (c) 2008 Filament Group, Inc
 * Dual licensed under the MIT (filamentgroup.com/examples/mit-license.txt) and GPL (filamentgroup.com/examples/gpl-license.txt) licenses.
 * --------------------------------------------------------------------
 */
 
jQuery.download = function(url, data, method){
    logger.debug('GDP: User is downloading a file.')
    //url and data options required
    //if( url && data ){
    //data can be string of parameters or array/object
    data = typeof data == 'string' ? data : jQuery.param(data);
    //split params into form inputs
    var inputs = '';
    jQuery.each(data.split('&'), function(){ 
        var pair = this.split('=');
        inputs+='<input type="hidden" name="'+ pair[0] +'" value="'+ pair[1] +'" />'; 
    });
    
    // http://internal.cida.usgs.gov/jira/browse/GDP-505
    var toggleUnload =  !($.browser.msie  && parseInt($.browser.version) == 7) && Constant.ui.view_pop_unload_warning == 1;
    
    if (toggleUnload) {
        window.onbeforeunload = undefined; // http://internal.cida.usgs.gov/jira/browse/GDP-378
    }
    jQuery('<form action="'+ url +'" method="'+ (method||'post') +'">'+inputs+'</form>').appendTo('body').submit().remove();
    
    if (toggleUnload) {
        window.onbeforeunload = function() { return "Leaving the Geo Data Portal will cause any unsaved configuration to be lost."; }
    }
};
