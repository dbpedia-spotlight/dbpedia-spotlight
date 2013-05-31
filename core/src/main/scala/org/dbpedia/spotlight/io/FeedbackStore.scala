package org.dbpedia.spotlight.io

import java.net.URL
import org.dbpedia.spotlight.model.{Text, SurfaceForm, DBpediaResource}
import java.io.PrintStream


/**
 * Stores user-provided feedback on annotations (correct, incorrect, etc.)
 *
 */
trait FeedbackStore {
    /*
     */
    def add(docUrl: URL, text: Text, resource: DBpediaResource, surfaceForm: SurfaceForm, offset: Int, feedback: String, systems: Array[String])
}

class CSVFeedbackStore(val output: PrintStream) extends FeedbackStore {
    def add(docUrl: URL, text: Text, resource: DBpediaResource, surfaceForm: SurfaceForm, offset: Int, feedback: String, systems: Array[String]) {
      output.println(List(docUrl,feedback,resource.uri,surfaceForm.name,text.text,offset,systems.mkString(" ")).mkString("\t"))
    }
}

