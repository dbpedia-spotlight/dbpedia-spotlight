/**
 * If you use this script, please give credit to DBpedia Spotlight by: 
 * - adding "powered by DBpedia Spotlight" somewhere in the page. 
 * - add a link to http://spotlight.dbpedia.org. 
 *
 * TODO people can also want to change the boundaries of the spotting
 * TODO showScores = [ list of score names to display ]
 *
 * @author pablomendes
 *
 */

(function( $ ){

   var powered_by = "<div style='font-size: 9px; float: right'><a href='http://spotlight.dbpedia.org'>Powered by DBpedia Spotlight</a></div>";

   var settings = {      
      'endpoint' : 'http://spotlight.dbpedia.org/dev/rest',
      'confidence' : 0.4,          //
      'support' : 20,
      'powered_by': 'yes',         // yes or no
      'showScores': 'yes',         // yes or no
      'types': '',
      'spotter': 'LingPipeSpotter' // one of: LingPipeSpotter,AtLeastOneNounSelector,CoOccurrenceBasedSelector
    };

   var methods = {
      init : function( options ) {           
          // If options exist, lets merge them with our default settings
          if ( options ) { 
              $.extend( settings, options );
          }
      },
      best: function( options ) {
          function update(response) { 
              console.log($(response));
               var content = $(response).find("div");  //the div with the annotated text
               if (settings.powered_by == 'yes') { 
                   $(content).append($(powered_by)); 
               };
               //var entities = $(content).find("a/[about]");
               $(this).html(content);
          }    
       
               return this.each(function() {            
                 console.log($.quoteString($(this).text()));
                 var params = {'text': $(this).text(), 'confidence': settings.confidence, 'support': settings.support, 'spotter': settings.spotter };
                 if("types" in settings){
                   params["types"] = settings.types;
                 }
    
                 $.ajax({ 'url': settings.endpoint+"/annotate", 
                      'data': params,
                      'context': this,
                      'headers': {'Accept': 'application/xhtml+xml'},
                      'success': update
                    });    
                 });
       },
       candidates: function( options ) {
          function getSelectBox(resources) {
             var snippet =  "<ul class='candidates'>";
             console.log(resources);
             var options = ""; $.each(resources, function(i, r) { 
                 options += "<li class='candidate-" + i + "'><a href='" + r["@uri"] + "' about='" + r["@uri"] + "'>" + r["@label"] + "</a>";
                 //TODO settings.showScores = ["finalScore"] foreach showscores, add k=v
                 if (settings.showScores == 'yes') options += " <span>(" + parseFloat(r["@finalScore"]).toPrecision(3) +")</span>";
                 options += "</li>"; 
             });
             snippet += options;
             snippet += "</ul>"
             return snippet;
          }

          
          function parseCandidates(response) {
             var json = $.parseJSON(response);
    
             var text = json["annotation"]["@text"];

             var start = 0;
             var annotatedText = text; 
            
             if (json.annotation['surfaceForm']!=undefined)  
                  var annotations = new Array().concat(json.annotation.surfaceForm) // deals with the case of only one surfaceFrom returned (ends up not being an array)
                  annotatedText = annotations.map(function(e) {
                  //console.log(e);
                  var sfName = e["@name"];
                  var offset = parseInt(e["@offset"]);
                  var sfLength = parseInt(sfName.length);
                  var snippet = text.substring(start, offset)
                  var surfaceForm = text.substring(offset,offset+sfLength);
                  start = offset+sfLength;
                  snippet += "<div id='"+(sfName+offset)+"' class='annotation'><a class='surfaceForm'>" + sfName + "</a>";
                  //TODO instead of showing directly the select box, it would be cuter to just show a span, and onClick on that span, build the select box.
                  snippet += getSelectBox($(e.resource));
                  snippet += "</div>";
                  return snippet;
			    }).join("");
             //snippet after last surface form
             annotatedText += text.substring(start, text.length);
             console.log(annotatedText);
             return annotatedText;
          }

               function update(response) { 
                   var content = "<div style='overflow: auto'>" + parseCandidates(response) + "</div>";
                   if (settings.powered_by == 'yes') { 
                       $(content).append($(powered_by)); 
                   };                        
                   $(this).html(content);          
               }    
       
               return this.each(function() {            
                 var params = {'text': $(this).text(), 'confidence': settings.confidence, 'support': settings.support, 'spotter': settings.spotter }; 
                 if("types" in settings && settings["types"] != undefined){
                   params['types'] = settings.types;
                 }
    
                 $.ajax({ 'url': settings.endpoint+"/candidates", 
                      'data': params,
                      'context': this,
                      'headers': {'Accept': 'application/json'},
                      'success': update
                    });
               });
       }

  }; 
  
  $.fn.annotate = function(method) {    
      // Method calling logic
      if ( methods[method] ) {
        return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
      } else if ( typeof method === 'object' || ! method ) {
        return methods.init.apply( this, arguments );
      } else {
        $.error( 'Method ' +  method + ' does not exist on jQuery.spotlight' );
      } 
  };
  
})( jQuery );
