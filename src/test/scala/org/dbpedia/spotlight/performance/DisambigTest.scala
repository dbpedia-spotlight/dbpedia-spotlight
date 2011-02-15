package org.dbpedia.spotlight.performance

import org.dbpedia.spotlight.disambiguate.DefaultDisambiguator
import org.dbpedia.spotlight.string.ParseSurfaceFormText
import org.dbpedia.spotlight.evaluation.Profiling
import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 03.02.11
 * Time: 18:40
 * To change this template use File | Settings | File Templates.
 */

object DisambigTest {

    def main(args: Array[String]) {
        val s = """ Because of their endearing (if quirky) personalities and their widespread popularity, the characters have been described as positive international [[icon]]s of modern [[culture of the United Kingdom|British culture]] in particular and of the [[British people]] in general. [[BBC News]] has called them "some of the best-known and best-loved stars to come out of the UK".<ref name=youngs>[http://news.bbc.co.uk/1/hi/entertainment/film/4295048.stm Wallace and Gromit's cracking careers]. By Ian Youngs. [[BBC News]]. Published 10 October 2005.</ref> [http://www.icons.org.uk/ ''Icons''] has said they have done "more to improve the image of the English world-wide than any officially appointed ambassadors".<ref>[http://www.icons.org.uk/nom/nominations/wallacegromit Wallace and Gromit]. [http://www.icons.org.uk/ icons.org.uk]. Retrieved 7 June 2009.</ref> The short films ''[[The Wrong Trousers]]'' and ''[[A Close Shave]]'' and the full length feature ''[[Wallace & Gromit: The Curse of the Were-Rabbit|The Curse of the Were-Rabbit]]'' received [[Academy Award]]s. The first short film ''[[A Grand Day Out]]'', was nominated for the [[Academy Award for Best Animated Short Film]], but lost to ''[[Creature Comforts]]'', another animated creation of Nick Park, and the most recent short film ''[[A Matter of Loaf and Death]]'' was also nominated in 2010. Each of the films has recieved critical acclaim, with all four of the short films having 100% positive rating on aggregator site Rotten Tomatoes, and the feature film having a 95%, putting it on the top 20 highest acclaimed animated feature films on the site.

The two characters appear in the monthly ''[[BeanoMAX]]'' comic. This eventually replaced their original comic published by [[Titan Magazines]]. They are also heavily featured in the unofficial (though supported by Aardman Animations) monthly free online magazine 'Aardmag'.<ref name=hamilton>[http://www.62westwallabystreet.co.uk 62 West Wallaby Street fansite]. By Seb Hamilton.</ref>

==Wallace==
Voiced by [[Peter Sallis]], and by Ben Whitehead in ''[[Wallace & Gromit's Grand Adventures]]'',<ref>{{Cite web|url=http://www.express.co.uk/posts/view/76520/Video-exclusive-Nick-Park-and-Peter-Sallis-bring-Wallace-Gromit-to-TV |title=Daily Express &#124; Film Review :: Video exclusive: Nick Park and Peter Sallis bring Wallace & Gromit to TV |publisher=Express.co.uk |date=2008-12-21 |accessdate=2009-07-20}}</ref> Wallace can usually be found wearing a white shirt, brown wool trousers, green knitted [[sleeveless sweater|pullover]], and a red tie. He is best known for his love of [[cheese]], especially Wensleydale,<ref>{{Cite web|url=http://www.wallaceandgromit.net/wensleydale.php |title=WallaceAndGromit.net |publisher=WallaceAndGromit.net |date= |accessdate=2009-07-20}}</ref> and [[cracker (food)|crackers]].  His birthday is 7 August. The thought of [[Lancashire hotpot]] keeps him going in a crisis. He enjoys [[tea]] or a drop of [[Bordeaux wine|Bordeaux red]] for special occasions. He reads the ''Morning, Afternoon and Evening Post'' and occasionally ''Ay-Up!'', which is a parody on ''[[Hello!]]'' magazine.

Wallace is an inveterate [[invention|inventor]], creating elaborate contraptions that often do not work as intended. He is a self-proclaimed [[genius]], evident from his exclamation when he discovers [[Hutch (Wallace and Gromit)|Hutch]]'s borrowed skill, a talent for all things mechanical. Most of Wallace's inventions look not unlike the designs of [[W. Heath Robinson]] and [[Rube Goldberg]], and Nick Park has said of Wallace that all his inventions are designed around the principle of using a [[sledgehammer]] to crack a [[nut (fruit)|nut]].  Wallace's official job varies; in ''A Close Shave'' he is a window washer.  In ''The Curse of the Were-Rabbit,'' Wallace is a pest exterminator, although he keeps the rabbits he catches. In the most recent short he is a baker.

Some of Wallace's contraptions are based on real inventions. For example, his method of getting up in the morning incorporates a bed that tips over to wake up its owner, an invention that was exhibited at [[The Great Exhibition]] of 1851 by [[Theophilus Carter]], and is similar to a device sold in Japan that is used to ensure the sleeper awakens on time by inflating a pillow under their normal pillow and rolling the person, thus waking them up.

He has a kindly nature, and is perhaps a little over-optimistic. At times he can be a little selfish and inconsiderate, but he has a good heart and always means well.  Nick Park, his creator, says: "He's a very self-contained figure. A very homely sort who doesn't mind the odd adventure." He is loosely based on Nick Park's father,<ref>{{cite news|url=http://news.bbc.co.uk/1/hi/talking_point/forum/2314009.stm |title=Talking Point &#124; Forum &#124; Ask Wallace and Gromit creator: Nick Park |publisher=BBC News |date=2002-10-15 |accessdate=2009-07-20}}</ref> whom Nick described in a radio interview as "an incurable tinkerer". He described one of his father's constructions, a combination beach hut and trailer, as having curtains in the windows, bookshelves on the walls, and full-sized furniture bolted to the floor. The way he often holds his hands, palms forward, fingers curled, is said{{by whom|date=November 2010}} to be based on an eccentric school teacher."""

        val sfOccs = ParseSurfaceFormText.parse(s)
        val d = new DefaultDisambiguator(new File("E:\\dbpa\\data\\index\\DisambigIndex.restrictedSFs.plusTypes-plusSFs"))

        Profiling.timed(Profiling.printTime("Disambiguation in ")) {
            d.disambiguate(sfOccs)
        }
    }

}