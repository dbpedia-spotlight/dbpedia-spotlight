package org.dbpedia.spotlight.string;

import com.google.common.base.Joiner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.Text;

import java.io.IOException;
import java.io.StringReader;

/**
 * This is a very simple class that follows some specified policy to extract context words from a SurfaceFormOccurrence
 * It can be used in LuceneManager.getQuery(SurfaceForm sf, Text context)
 * 
 */
public class ContextExtractor {

    private String getRemainingTokens(String slice, int nTokens) {
        StringBuffer buffer = new StringBuffer();
        String[] tokens = slice.split("\\s+");
        for (String t: tokens) {
            buffer.append(t);
            buffer.append(" ");
        }
        return buffer.toString().trim();
    }
    /*
        TODO best way would be to implement this method recursively to try to fill in minTokens until string is exhausted.
        had to code it up quickly just to have a feel if it can work
      */
    private String getEnoughContext(int i, String[] text, int minTokens, int maxTokens) {
        StringBuffer enoughContext = new StringBuffer();
        String snippet = text[i];
        int nRemainingTokens = (maxTokens - snippet.length()) / 2;

        if (snippet.split("\\s+").length > minTokens) // termination condition: snippet is large enough
            return snippet;
        else { // if it's too small, add more snippets
            if (i>0) {                      // add previous snippet
                enoughContext.append(getRemainingTokens(text[i-1], nRemainingTokens));
                enoughContext.append(" ");
            }
            enoughContext.append(snippet);   // add current snippet
            if (i<text.length-1) {
                enoughContext.append(" ");   // add next snippet
                enoughContext.append(getRemainingTokens(text[i+1], nRemainingTokens));
            }
        }

        //HACK
        if (enoughContext.length() < minTokens)
            return Joiner.on(" ").join(text);

        return enoughContext.toString();

    }

    public Text narrowContext(SurfaceForm sf, Text context) {
        String enoughContext = context.text();
        String[] snippets = context.text().toLowerCase().split("\n");
        int i = 0;
        for (String snippet: snippets) {
            if (snippet.contains(sf.name().toLowerCase())) {
                enoughContext = getEnoughContext(i, snippets, 20, 1000) ;
            }
            i++;
        }
        return new Text(enoughContext);
    }

    public void analyze(String text, Analyzer analyzer) throws IOException {
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
        while (true) {
            Token token = stream.next();
            if (token == null) break;

            System.out.print("[" + token.termText() + "] ");
        }
        System.out.println("\n");
    }



    public static void main(String[] args) throws SearchException {

        String[] text1 = {"t1 t2 t3 t4 t5 t6 t7 t8 t9 t10", "a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 a21 ", "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10"};

        String[] text = text1;
        ContextExtractor e = new ContextExtractor();
        for (int i=0; i<text.length; i++)
            System.out.println(e.getEnoughContext(i, text1, 10, 100));

        System.out.println();
        //System.out.println(e.narrowContext(new SurfaceForm("a12"), new Text(Joiner.on("\n").join(text1))));

        Text t2 = new Text("WASHINGTON - The nation's homeland security chief pledged Wednesday that 75 metropolitan areas would have advanced disaster communications systems by 2009 but acknowledged that friction among emergency agencies continues to hinder progress.  \"We are determined to get this job done in the next two years,\" Homeland Security Secretary Michael Chertoff said at a news briefing.  He did not say how much the effort would cost, but so far the government has distributed more than $2.9 billion to communities around the country with mixed results. Democrats who will control Congress this year have also promised to make communications upgrades a priority, but have provided little detail.  Chertoff released a survey of how well the 75 U.S. communities have prepared their first responders to communicate during a catastrophe. More than five years after the attacks of Sept. 11, 2001, exposed severe weaknesses in the ability of emergency workers to communicate with each other, the report gave only six of the areas studied the highest grades.  Portions of the report had been obtained Tuesday by The Associated Press.  â€˜Longstanding cultural differences' Chertoff said Wednesday that since the Sept. 11 terrorist attacks, agencies have tried to overcome old turf rivalries that can hamper emergency work.  \"In some communities, not all, there are some longstanding cultural differences between different kinds of responders,\" including police, firefighters and medical personnel, Chertoff said. \"I think that is a challenge and that culture has been a challenge.\"  The report found that while emergency agencies in more than 60 percent of the communities studied had the ability to talk to each other during a crisis, only 21 percent overall showed \"the seamless use\" of equipment needed to also communicate with state and federal officials.  The report's highest ratings went to the Washington, D.C., area; San Diego; Minneapolis-St. Paul; Columbus, Ohio; Sioux Falls, S.D.; and Laramie County, Wyo.  The lowest scores went to Chicago; Cleveland; Baton Rouge, La.; Mandan, N.D.; and American Samoa. The report includes large and small cities and their suburbs, along with U.S. territories.  In an overview, the report says all 75 areas surveyed have policies in place for helping their emergency workers communicate. But it also finds that \"formalized governance (leadership and planning) across regions has lagged.\"  The study is likely to add fuel to what looms as a battle in Congress this year. Democrats who take over the majority this week have promised to try fixing the problem emergency agencies have communicating with each other but have not said specifically what they will do, how much it will cost or how they will pay for it.  \"Five years after 9/11, we continue to turn a deaf ear to gaps in interoperable communications,\u0006 - the term used for emergency agencies' abilities to talk to each other, said Sen. Charles Schumer, D-N.Y. \"If it didn't have such potentially devastating consequences, it would be laughable.\"  The attacks of Sept. 11, 2001, revealed major problems in how well emergency agencies were able to talk to each other during a catastrophe. Many firefighters climbing the World Trade Center towers died when they were unable to hear police radio warnings to leave the crumbling buildings.  The report says first responders in New York now have well-established systems to communicate with each other - but not the best, most advanced possible. Thirteen U.S. cities score better than New York.  Judged in three categories Communities were judged in three categories: operating procedures in place, use of communications systems and how effectively local governments have coordinated in preparation for a disaster.  Most of the areas surveyed included cities and their surrounding communities, based on the assumption that in a major crisis emergency personnel from all local jurisdictions would respond.  The areas with the six best scores were judged advanced in all three categories. The cities with the lowest grades had reached the early implementation stage for only one category, and intermediate grades in the other two categories.  The Chicago Office of Emergency Management and Communications took issue with the report's methodology and findings.  \"We strongly disagree with the results of this study, and feel that the parameters of the study were inconsistent and limited,\" the statement said.  Tammy Lapp, the emergency coordinator for Mandan and Morton County, N.D., said she was not surprised by their low ranking on the scorecard.  \"We knew with our limited funds, we were going to fall short,\" she said. \n" +
                "Five healthy resolutions for 2007 Follow nutritionist Joy Bauer's recommendations for a better year    Have you made a New Year's resolution yet? Have you broken it already? Don't despair. Nutritionist Joy Bauer has created 5 healthy, and simple, resolutions we all should have on our \"to do\" list for the new year:  Joy's Healthy Resolutions for 2007:  1. Make YOURSELF a priority *Shop for healthy foods your body needs *Get enough sleep *Exercise regularly  2. Know your numbers Ring in the New Year with a pledge to visit your doctor and learn your blood pressure reading and fasting cholesterol, triglyceride and blood sugar levels.  The sooner you know where you stand, the sooner you can take action to make corrections.  *  Optimal blood pressure equal to or < 120 / 80 mmHg *  Total cholesterol  < 200 mg/dL *  LDL-Cholesterol  < 100 mg/dL *  HDL-Cholesterol  40 mg/dL or higher *  Cholesterol ratio  below 5 *  Triglycerides  < 150 mg/dL *  Blood Sugars  < 100 mg/dL  3. Up your ante! Antioxidants are naturally occurring compounds found in plant based foods. Antioxidants help the body fight many serious conditions, including cancer and heart disease, by thwarting the action of harmful free radicals. According to a study published in the American Journal of Clinical Nutrition, July 2006, the following foods are among the richest in antioxidants. Go out of your way to incorporate them into your daily diet.  1. Blackberries 2. Walnuts 3. Strawberries 4. Artichokes  5. Cranberries 6. Raspberries 7. Blueberries 8. Cloves, ground  4. Incorporate whole grains 2007 is the year to stop buying refined, white starch. Instead, embrace brown rice, whole wheat pasta, whole grain bread and other healthy grain varieties like millet, faro, and amaranth.  Learn how to buy the right bread! Many consumers believe they're buying the right type of bread but are often confused. According to a Sara Lee Food & Beverage survey, 73% of consumers who were asked what type of bread they had in the house said \"whole wheat bread,\" but after checking the label realized that they mistakenly thought \"enriched wheat\" meant whole grain.  Look a \"whole\" lot closer at labels in the bread aisle - it must say 100% Whole Wheat or 100% Whole Grain, or list the word WHOLE as one of the first ingredients on the label.  5.   Slow the pace - lose fat on your waist In an interesting experiment that was presented at the 2006 Annual Scientific Meeting a NAASO, women were invited to eat a pasta lunch on two different days. On one day, the women were given a small spoon and asked to take small bites, put the spoon down between bites, and chew each bite at least 15 times. On the second day, the women were given a large spoon and asked to eat as quickly as possible without stopping between bites. On both occasions, the women ate until they felt comfortably full. When researchers measured how many calories the women ate, they found that women ate 67 more calories when they ate quickly than when they ate slowly. Although this may not sound like much, calories add up.  An additional 134 calories per day, means 48,910 calories per yearâ€¦. that's potentially 14 pounds (lost or gained!).  For more information on healthy eating, visit nutrition expert, Joy Bauer's website at www.joybauernutrition.com \n" +
                "Gerald Ford laid to rest in Michigan Burial of ex-president, 'hometown hero' caps eight days of mourning    GRAND RAPIDS, Mich.  - Gerald R. Ford was laid to rest on the grounds of his presidential museum Wednesday after eight days of mourning and remembrance that spanned the country, from the California desert to the nation's capital and back to Ford's boyhood home.  The sunset burial capped the official mourning for the 38th president after a 17-hour viewing Tuesday night and Wednesday at the museum in his hometown.  At a graveside service that included a 21-gun salute and a 21-aircraft flyover, Vice President Dick Cheney presented former first lady Betty Ford with the American flag that was draped over her husband's casket.  Earlier, Ford was remembered as a man not afraid to laugh, make tough decisions or listen to the advice of his independent wife in eulogies delivered during a funeral at the church the couple attended for six decades.  An honor guard carried the casket inside Grace Episcopal Church, where Ford's defense secretary, Donald Rumsfeld, and Ford's successor, Jimmy Carter, recalled his public service.  Rumsfeld: 'He was one of us' His widow wiped away tears as she sat with the couple's four children and more than 300 dignitaries and family friends, including golfing legend Jack Nicklaus.  \"He was one of us,\" Rumsfeld said, \"And that made him special and needed in a dark and dangerous hour for our nation.\"  Rumsfeld, who recently left his post as President Bush's defense secretary, remembered Ford as a courageous and steady leader who healed the nation after Watergate.  Rumsfeld said the Navy is considering naming a new aircraft carrier after Ford, a Navy veteran. A decision is expected later this month.  \"How fitting it would be that the name Gerald R. Ford will patrol the high seas for decades to come in defense of the nation he loved so much,\" he said.  Carter described the close personal friendship he and Ford developed over the years.  \"I relished his sound advice,\" Carter said as his wife, Rosalynn, cried. \"I want to thank my predecessor for all he did to heal our land.\"  Thousands of flag-waving mourners lined the roads under sunny skies as the motorcade bearing Ford's casket traveled between his presidential museum in downtown Grand Rapids to the church, before returning to the museum.  Viewing period extended The viewing had to be extended Wednesday until nearly noon so everyone in line could pay their respects. Some 57,000 mourners waited hours to file past the flag-draped casket during the night. Some stopped and made silent prayers.  \"We're here to honor him,\" said Philip Bareham of Lansing, who was the last person to view the casket and whose parents were among Ford's earliest supporters and political allies. \"We just love this family. They are so down to earth.\"  Ford represented Grand Rapids in Congress for 25 years. His family had belonged to Grace Episcopal Church since the early 1940s.  Richard Norton Smith, an author, presidential historian and former director of Ford's museum and library, reminded mourners how important Ford's hometown was to him.  \"Grand Rapids returned his affection many times over,\" which was \"unforgettably demonstrated by the tens of thousands who stood in line for hours outside the museum, braving the cold to assure that his last night was anything but lonely,\" Smith said.  Draped over the back of one pew at the funeral was a blue blanket with the letter \"M\" emblazoned on it, symbolizing Ford's alma mater, the University of Michigan, where he played football for national championship teams in 1932 and 1933.  Many of the mourners at the museum and lining the roads during his funeral procession on Wednesday wore Michigan hats and sweat shirts in his honor.  Ford, who became president after Richard Nixon resigned, died Dec. 26 at his home in Rancho Mirage, Calif. He was 93. \n" +
                "Health insurance bridges gap for poor families Free medical care can help children get out of poverty, official says ");

        System.out.println(e.narrowContext(new SurfaceForm("GRAND RAPIDS"), t2));

    }
}
