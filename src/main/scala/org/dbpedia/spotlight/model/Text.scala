package org.dbpedia.spotlight.model

/**
 * Text represents a the input string for spotlight. It can be a document, paragraph or just a text snippet.
 * It basically holds a string and an optional identifier. Subclasses can add more attributes.
 * Our pipeline operates on objects of this class by adding annotations.
 */

class Text(var text : String)
{
    // do some clean up on the text
    text = text.replace("’", "'")


    def equals(that : Text) =
    {
        text.equals(that.text)
    }
    
    override def toString = "Text["+text+"]"
}
