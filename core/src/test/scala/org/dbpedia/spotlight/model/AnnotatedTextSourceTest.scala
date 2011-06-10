package org.dbpedia.spotlight.model

import org.junit.Test
import org.junit.Assert._
import org.dbpedia.spotlight.io.AnnotatedTextSource
import java.io.File

/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

/**
 * Partial list of tests for class AnnotatedTextSource
 * -- it will read from both occurrence tsv files and paragraph tsv files (undefined as of yet)
 * @author pablomendes
 */

class AnnotatedTextSourceTest {

    val exampleOccs = "Terra_Vibe_Park-p10l10\tMarilyn_Manson\tMarilyn Manson\tIron Maiden Black Sabbath Velvet Revolver Black Label Society  Dragonforce Marilyn Manson  Moby Slayer  Accept  Candlemass  Twisted Sister  Dio  Anthrax Cure  Korn\t75\nFox_%28crater%29-p1l4\tMoon\tMoon\t Fox is a small lunar crater on the far side of the Moon. It lies near the northern rim of the crater Wyld, and to the southeast of Babcock. This crater is bowl-shaped, with a roughly circular rim, simple sloping walls and a relatively level, featureless interior. There is some talus along the northern inner wall.\t52\nMilitary_Knights_of_Windsor-p4l3\tOrder_of_the_Garter\tOrder of the Garter\tThe Military Knights of Windsor were constituted by King Edward III following the Battle of Crécy, when many knights captured by the French were forced to liquidate their estates to raise ransom money in order to secure their release.  They are therefore sometimes also known as Alms Knights, or Poor Knights.  At the original establishment of the Order of the Garter, twenty-six Poor Knights were appointed and attached to the Order and its chapel at St. George's Chapel, Windsor. The number was not always maintained; by the seventeenth century, there were just thirteen Poor Knights. At his restoration, King Charles II increased the number to eighteen. After they objected to being termed \"poor\", King William IV renamed them the Military Knights of Windsor.\t348\nBroadcasting_in_the_Soviet_Union-p7l2\tInternational_Radio_and_Television_Organisation\tOIRT\tBroadcast radio Although the Soviet Union had domestic shortwave stations, most of the radio stations operated in the AM band. In typical Soviet fashion, neither the sites nor the frequencies of domestic AM or SW stations were ever disclosed, thus leaving shortwave listeners wanting to tune into Soviet radio to memorize the frequencies and remember where the sites were. However, the AM/SW programming was relayed on FM, using the OIRT FM band (66-73 MHz).\t433\nRomanian_Canadian-p27l18\tAlberta\tAlberta\tTimeline 1896-1900 – A group of Romanians established themselves to the Saskatchewan, at Clifford Sifton's advice. 1898 – The first two Romanian families that migrated to Canada from the Bukovina village of Boian stopped in Alberta. They gave the settlement the name Boian, Alberta. 1939 – On Iberville Street, in Montreal, was built \"Casa Romana\", where was set up a Romanian school. 1952 – The Romanian Association of Canada (A.R.C.) founded in Montreal by Gheorghe Loghiade ( -1986), Gheorghe Stanciu, Petre Sultana, Miron Georgescu, Nichita Tomescu, Florin Marghescu, Ion Ţăranu (1921–2009), Alexandru Fonta (1922–2004) and Mihai Pop. The association was incorporated in 1953. 1965 – The Romanian Association of Canada launches fund raising events in order to build the Romanian Orthodox Church \"Buna Vestire\", situated on Cristoph Colomb Street in Montreal. 1970 – launches  fund raising events to help flood victims in Romania. 1970 – Alexandru Fonta (1922–2004), Vasile Posteucă (1912–1972) and Jean Ţăranu (1921–2009) donate a piece of land known today as \"The Romanian Camp\" in Val-David, Quebec. In 1980 in Val-David are inaugurated two Romanian landmarks, the Predeal-Trudeau Street and the Romanians Bridge. 1971 – A.R.C. launches the first Romanian Radio Show called \"Ora de radio\". Since 1999\t224\n1902_in_baseball-p15l10\tChicago_Cubs\tChicago Cubs\tSeptember September 1 - The Chicago Cubs famed trio of Joe Tinker, Johnny Evers and Frank Chance appear in the Chicago lineup for the first time together. September 10 - Rube Waddell of the Philadelphia Athletics, making only 6 relief appearance all season, does it twice in a double-header against the Baltimore Orioles and gets the victory in both games. September 20 - Nixey Callahan of the Chicago White Sox pitches a no-hitter against the Detroit Tigers.\t28\nPaul_Priestly-p3l2\tLeslie_Grantham\tLeslie Grantham\t Character creation and development  1989 was a year of big change for EastEnders, both behind the cameras and in front of them. Original production designer, Keith Harris, left the show, and co-creators, Tony Holland and Julia Smith, both decided that the time had come to move on too; their final contribution coinciding with the exit of one of EastEnders most successful characters, Den Watts (Leslie Grantham). A new producer, Mike Gibbon, was given the arduous task of taking over the show and he enlisted the most experienced writers to take over the storylining of the programme, including Charlie Humphreys, Jane Hollowood and Tony McHale.\t397\nHistory_of_fashion_design-p17l2\tFlapper\tflapper\t1920s Soon after the First World War, a radical change came about in fashion. Bouffant coiffures gave way to short bobs, dresses with long trains gave way to above-the-knee pinafores. Corsets were abandoned and women borrowed their clothes from the male wardrobe and chose to dress like boys. Although, at first, many couturiers were reluctant to adopt the new androgynous style, they embraced them wholeheartedly from around 1925. A bustless, waistless silhouette emerged and aggressive dressing-down was mitigated by feather boas, embroidery, and showy accessories. The flapper style (known to the French as the 'garçonne' look) became very popular among young women. The cloche hat was widely-worn and sportswear became popular with both men and women during the decade, with designers like Jean Patou and Coco Chanel popularizing the sporty and athletic look. \t572\nAsuka_Hinoi-p5l10\tTomoko_Kawase\tTomoko Kawase\tLater in 2003, Tomoko Kawase, better known as Tommy february6, had seen potential in Asuka and had produced her first solo single, ♥Wanna be your girlfriend♥, under the Sonic Groove label, a sub-label of avex trax. The song was used as the ending theme for the drama Ashita Tenki ni Naare. The single only had made it to position 60 on the Oricon charts, but did give Asuka much needed exposure. Five months later, her second single , was written by the famous songwriter behind SPEED, Hiromasa Ijichi. Her second single had given her much more exposure as she performed on musical shows like Music Station, Pop Jam, and HEY!HEY!HEY!. This single did not do as well as her first on the charts, as it only reached position number 79 on the Oricon charts.\t15\nPortal:Lower_Saxony/Index-p30l1\tWelf_pudding\tWelf Pudding\tFood: Lower Saxon cuisine - Asparagus - Birnen, Bohnen und Speck - Braunschweiger - Bregenwurst - Brunswick Mum - Grünkohlessen - Heidschnucke - Knipp - Korn - Pinkel - Welf Pudding\t169\n";
    @Test
    def fromString {
        val paragraphs = AnnotatedTextSource.fromOccurrencesString(exampleOccs)
        assertTrue(paragraphs.size > 0)
    }

    val exampleRepeats = "Frankfort-pl16\tFrankfort_%28village%29,_New_York\tFrankfort\tFrankfort (village), New York, within the town of Frankfort\t0\nFrankfort-pl16\tFrankfort_%28village%29,_New_York\tFrankfort\tFrankfort (village), New York, within the town of Frankfort\t0\nFrankfort-pl16\tFrankfort_%28village%29,_New_York\tFrankfort\tFrankfort (village), New York, within the town of Frankfort\t0\nFrankfort-pl16\tFrankfort_%28village%29,_New_York\tFrankfort\tFrankfort (village), New York, within the town of Frankfort\t0\nFrankfort-pl16\tFrankfort_%28village%29,_New_York\tFrankfort\tFrankfort (village), New York, within the town of Frankfort\t0\n"
    @Test
    def repeats {
        val paragraphs = AnnotatedTextSource.fromOccurrencesString(exampleRepeats)
        assertEquals(1, paragraphs.size)
    }

//    @Test
//    def fromFile {
//        val paragraphs = AnnotatedTextSource.fromOccurrencesFile(new File("/home/pablo/eval/test1mio.random.tsv"))
//        assertTrue(paragraphs.size > 0)
//    }
}