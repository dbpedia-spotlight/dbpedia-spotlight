package org.dbpedia.spotlight.string

import org.dbpedia.spotlight.model._

/**
 * User: Max
 * Small test class for narrowing the context window.
 */

object TestContextUtil {

    def test(contextor : ContextExtractor, occ : SurfaceFormOccurrence, numberOfWords : Int) {
        val newOcc = contextor.narrowContext(occ)

        println("Text: "+newOcc.context.text)

        assert(newOcc.context.text.substring(newOcc.textOffset).startsWith(newOcc.surfaceForm.name), "surface form not at offset")

        val words = newOcc.context.text.split("\\s+").length
        assert(words < numberOfWords+2, "too many words: got "+words+", needed "+numberOfWords)
        assert(words > numberOfWords-2, "not enough words: got "+words+", needed "+numberOfWords)
    }


    def main(args : Array[String]) {

        val t0 = new SurfaceFormOccurrence(
            new SurfaceForm("congregated"),
            new Text("congregated, you know. And we even have more words to add here in the hope that it gets cut."),
            0
        )
        test(new ContextExtractor(5, 10), t0, 10)

        val t1 = new SurfaceFormOccurrence(
            new SurfaceForm("imposed"),
            new Text("As the throngs congregated, Egypt's Nile TV showed live pictures of car traffic crossing a bridge over the Nile River, with bulletins announcing that a curfew would be imposed again on Tuesday starting at 3 p.m.\n\nSoldiers and plain-clothed security officers searched demonstrators and controlled access to Tahrir, or Liberation Square, which has been the focal point of protests against Egypt's embattled Mubarak, 82."),
            168
        )
        test(new ContextExtractor(5, 10), t1, 10)

        val t2 = new SurfaceFormOccurrence(
            new SurfaceForm("congregated"),
            new Text("As the throngs congregated, Egypt's Nile TV showed live pictures of car traffic crossing a bridge over the Nile River, with bulletins announcing that a curfew would be imposed again on Tuesday starting at 3 p.m.\n\nSoldiers and plain-clothed security officers searched demonstrators and controlled access to Tahrir, or Liberation Square, which has been the focal point of protests against Egypt's embattled Mubarak, 82.\n\nAfter the first lines of security, scores of self-appointed volunteers from the protest movement conducted their own searches of bags, and ordered arriving people to show their identity cards as they streamed in.\n\nOne man handed out leaflets, urging demonstrators to behave in front of foreign journalists covering the event."),
            15
        )
        test(new ContextExtractor(30, 100), t2, 100)

        val t3 = new SurfaceFormOccurrence(
            new SurfaceForm("congregated"),
            new Text("As the throngs congregated, Egypt's Nile TV showed live pictures of car traffic."),
            15
        )
        test(new ContextExtractor(30, 100), t3, 100)

        println("\n*** PASSED all tests ***")
    }

}