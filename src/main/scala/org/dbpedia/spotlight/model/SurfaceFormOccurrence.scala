package org.dbpedia.spotlight.model

class SurfaceFormOccurrence(val surfaceForm : SurfaceForm,
                            val context : Text,
                            val textOffset : Int,
                            val provenance : Provenance.Value,
                            var spotProb : Double = -1)
{

    def this(surfaceForm : SurfaceForm, context : Text, textOffset : Int) =
    {
        this(surfaceForm, context, textOffset, provenance = Provenance.Undefined)
    }

    def equals(that : SurfaceFormOccurrence) : Boolean =
    {
        (  this.surfaceForm.equals(that.surfaceForm)
        && this.context.equals(that.context)
        && (this.textOffset == that.textOffset) )
    }
    
    override def toString = {
        val span = 50
        val start = if (textOffset < span) 0 else textOffset-span
        val end = if (textOffset+span > context.text.length) context.text.length else textOffset+span
        val text = "Text[... " + context.text.substring(start, end) + " ...]"
        surfaceForm+" - at position *"+textOffset+"* in - "+text
    }

}