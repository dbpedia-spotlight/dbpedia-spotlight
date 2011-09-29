/*
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

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

// adding this in case we forget to remove some console.log statement that would make it break on firefox without firebug.
if (console===undefined) {
    console = { "log": function() {} }
}

//from http://www.wrichards.com/blog/2009/02/jquery-sorting-elements/
jQuery.fn.sort = function() {
    return this.pushStack( [].sort.apply( this, arguments ), []);
};
function sortOffset(a,b){
     if (a["@offset"] == b["@offset"]){
       return 0;
     }
     return parseInt(a["@offset"]) > parseInt(b["@offset"]) ? 1 : -1;
};


(function( $ ){

   var powered_by = "<div style='font-size: 9px; float: right'><a href='http://spotlight.dbpedia.org'>Powered by DBpedia Spotlight</a></div>";

   var settings = {      
      'endpoint' : 'http://localhost:2223/rest',
      'confidence' : 0.4,          //
      'support' : 20,
      'powered_by': 'yes',         // yes or no
      'showScores': 'yes',         // yes or no
      'types': '',
      'policy' : 'whitelist',
      'spotter': 'LingPipeSpotter', // one of: LingPipeSpotter,AtLeastOneNounSelector,CoOccurrenceBasedSelector
      'disambiguator': 'Default' // one of: LingPipeSpotter,AtLeastOneNounSelector,CoOccurrenceBasedSelector
    };

   function getScoreDOMElements(data) {
        var li = ""
        for (var key in data)
            li  += "<span class='hidden "+ key +"'>" + data[key] +"</span>";
        return li;
   }

   var Parser = {
	getSelectBox: function(resources, className) {
             var ul =  $("<ul class='"+className+"s'></ul>");
             $.each(resources, function(i, r) {
                 var li = "<li class='"+className+" "+className+"-" + i + "'><a href='http://dbpedia.org/resource/" + r["@uri"] + "' about='" + r["@uri"] + "'>" + r["@label"] + "</a>";

                 //TODO settings.showScores = ["finalScore"] foreach showscores, add k=v
                 if (settings.showScores == 'yes') {
                    li += " (<span class='finalScoreDisplay'>" + parseFloat(r["@finalScore"]).toPrecision(3) +"</span>)";
                 }

                 li += getScoreDOMElements({
                                     "finalScore": parseFloat(r["@finalScore"]).toPrecision(3),
                                     "contextualScore": parseFloat(r["@contextualScore"]),
                                     "percentageOfSecondRank": parseFloat(r["@percentageOfSecondRank"]),
                                     "support": parseFloat(r["@support"]),
                                     "priorScore": parseFloat(r["@priorScore"])
                                    });
                 
                 li += "</li>";
                 var opt = $(li);
                 $(ul).append(opt);
             });
             return ul;
        },
       getAnnotatedText: function(response) {
           var json = $.parseJSON(response);
           if (json==null) json = response; // when it comes already parsed

           var text = json["annotation"]["@text"];

           var start = 0;
           var annotatedText = text;

           var annotations = new Array();
           if (json.annotation['surfaceForm']!=undefined) {
               annotations = annotations.concat(json.annotation.surfaceForm).sort(sortOffset) // deals with the case of only one surfaceFrom returned (ends up not being an array)
           } else {
               //TODO show a message saying that no annotations were found
           }
           annotatedText = annotations.map(function(e) {
               if (e==undefined) return "";
               //console.log(e);
               var sfName = e["@name"];
               var offset = parseInt(e["@offset"]);
               var sfLength = parseInt(sfName.length);
               var snippet = text.substring(start, offset)
               var surfaceForm = text.substring(offset,offset+sfLength);
               start = offset+sfLength;

               var classes = "annotation";

               snippet += "<div id='"+(sfName+offset)+"' class='" + classes + "'><a class='surfaceForm'>" + sfName + "</a>";
               var ul = Parser.getSelectBox($(e.resource),'candidate');
               //ul.children().each(function() { console.log($.data($(this),"testProp")); });
               snippet += "<ul class='candidates'>"+ul.html()+"</ul>"; //FIXME this wrapper is a repeat from getSelectBox
               snippet += "</div>";

               return snippet;
           }).join("");
           //snippet after last surface form
           annotatedText += text.substring(start, text.length);
           //console.log(annotatedText);
           return annotatedText.replace(/\n/g, "<br />\n");
       },
        getAnnotatedTextFirstBest: function(response) {
            var json = $.parseJSON(response);
            if (json==null) json = response; // when it comes already parsed

            var text = json["@text"];

            var start = 0;
            var annotatedText = text;

            var annotations = new Array();
            if (json['Resources']!=undefined) {
                annotations = annotations.concat(json['Resources']); // deals with the case of only one surfaceFrom returned (ends up not being an array)
            } else {
                //TODO show a message saying that no annotations were found
            }

            annotatedText = annotations.map(function(e) {
                if (e==undefined) return "";

                var sfName = e["@surfaceForm"];
                var offset = parseInt(e["@offset"]);
                var uri = e["@URI"];

                var sfLength = parseInt(sfName.length);
                var snippet = text.substring(start, offset)
                var surfaceForm = text.substring(offset,offset+sfLength);
                start = offset+sfLength;

                var support = parseInt(e["@support"]);
                var confidence = 0.0;

                var classes = "annotation"
                snippet += "<a id='"+(sfName+offset)+"' class='" + classes + "' about='" + uri + "' href='" + uri + "' title='" + uri + "'>" + sfName

                snippet += getScoreDOMElements({
                    "finalScore": parseFloat(e["@similarityScore"]).toPrecision(3),
                    "contextualScore": parseFloat(e["@similarityScore"]).toPrecision(3), //TODO send contextualScore from server and grab here
                    "percentageOfSecondRank": parseFloat(e["@percentageOfSecondRank"]),
                    "support": parseFloat(e["@support"])
                });

                snippet += "</a>";

                return snippet;
            }).join("");
            //snippet after last surface form
            annotatedText += text.substring(start, text.length);
            //console.log(annotatedText);
            return annotatedText.replace(/\n/g, "<br />\n");
        },

	getSuggestions: function(response, targetSurfaceForm) {
             var json = $.parseJSON(response);
	     if (json==null) json = response; // when it comes already parsed
             var suggestions = "";
	     if (json.annotation['surfaceForm']!=undefined) {                  
                  var annotations = new Array().concat(json.annotation.surfaceForm) // deals with the case of only one surfaceFrom returned (ends up not being an array)
                  suggestions = annotations.filter(function(e) {
			return (e["@name"]==$.trim(targetSurfaceForm)); 
		  }).map(function(e) {
                  	return Parser.getSelectBox($(e.resource),'none');
		  }).join("");
            }
            return suggestions;
        }
   };

   var ajaxRequest;
   var methods = {
      init : function( options ) {           
          // If options exist, lets merge them with our default settings
          if ( options ) { 
              $.extend( settings, options );
          }
      },
      best: function( options ) {
          //init(options);
          function update(response) { 

                   var content = "<div>" + Parser.getAnnotatedTextFirstBest(response) + "</div>";
                   if (settings.powered_by == 'yes') {
                       $(content).append($(powered_by));
                   };
                   $(this).html(content);

               if(settings.callback != undefined) {
                   settings.callback(response);
               }
          }    
       
               return this.each(function() {            
                 //console.log($.quoteString($(this).text()));
                 var params = {'text': $(this).text(), 'confidence': settings.confidence, 'support': settings.support, 'spotter': settings.spotter, 'disambiguator': settings.disambiguator, 'policy': settings.policy };
                 if("types" in settings && settings["types"] != undefined)
                   params["types"] = settings.types;
                 if("sparql" in settings && settings["sparql"] != undefined)
                   params["sparql"] = settings.sparql;
    
                 ajaxRequest = $.ajax({ 'url': settings.endpoint+"/annotate",
                      'data': params,
                      'context': this,
                      'headers': {'Accept': 'application/json'},
                      'success': update,
                      'error': function(response) { if(settings.callback != undefined) { settings.callback(response); } }
                    });    
                 });
       },
       candidates: function( options ) {
		//init(options);
               function update(response) { 
                   var content = "<div>" + Parser.getAnnotatedText(response) + "</div>";
                   if (settings.powered_by == 'yes') { 
                       $(content).append($(powered_by)); 
                   };                        
                   $(this).html(content);

                   if(settings.callback != undefined) {
                    settings.callback(response);
                   }
               }    
       
               return this.each(function() {            
                 var params = {'text': $(this).text(), 'confidence': settings.confidence, 'support': settings.support, 'spotter': settings.spotter, 'disambiguator': settings.disambiguator, 'policy': settings.policy };
                 if("types" in settings && settings["types"] != undefined)
                   params['types'] = settings.types;
                 if("sparql" in settings && settings["sparql"] != undefined)
                   params["sparql"] = settings.sparql;
    
                 ajaxRequest = $.ajax({ 'url': settings.endpoint+"/candidates",
                      'data': params,
                      'context': this,
                      'headers': {'Accept': 'application/json'},
                      'success': update,
                      'error': function(response) { if(settings.callback != undefined) { settings.callback(response); } }
                    });
               });
       },
       suggest: function( options ) {
           //init(options);
           function update(response) {
               var keywords = Kolich.Selector.getSelected();
               var suggestion = Parser.getSuggestions(response, keywords);
               //console.log('keywords:'+keywords);
               //console.log('suggestion:'+suggestion);
               var content = "<div style='overflow: auto'>" + suggestion + "</div>";
               if (settings.powered_by == 'yes') {
                   $(content).append($(powered_by));
               };
               $(this).html(content);
           }

           return this.each(function() {
               return;
               //console.log('suggest');
               var keywords = $.trim("" + Kolich.Selector.getSelected());
               var params = {'text': $(this).text(), 'confidence': settings.confidence, 'support': settings.support, 'spotter': settings.spotter, 'disambiguator': settings.disambiguator, 'surfaceForm': keywords };
               if("types" in settings && settings["types"] != undefined){
                   params['types'] = settings.types;
               }

               ajaxRequest = $.ajax({ 'url': settings.endpoint+"/candidates",
                   'data': params,
                   'context': this,
                   'headers': {'Accept': 'application/json'},
                   'success': update,
                   'error': function(response) { if(settings.callback != undefined) { settings.callback(response); } }
               });
           });
       }

  }; 
  
  $.fn.annotate = function(method) {

	//console.log('method:',method);

      // Method calling logic
      if ( methods[method] ) {	
        return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
      } else if ( typeof method === 'object' || ! method ) {
        return methods.init.apply( this, arguments );
      } else {
        $.error( 'Method ' +  method + ' does not exist on jQuery.annotate (DBpedia Spotlight)' );
      } 
  };

    $.fn.cancelAnnotation = function() {
        ajaxRequest.abort();
    }
  
})( jQuery );
