/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.string

import java.util.regex.{Matcher, Pattern}
import org.apache.commons.lang.StringEscapeUtils

/**
 * Static class that strips all wiki and html markup from a string.
 * Most regular expressions are taken from Jimmy Lin's and David Milne's cloud9.
 */

object WikiMarkupStripper
{
    /**
     * Strips a string of all markup; tries to turn it into plain text
     *
     * @param markup the text to be stripped
     * @return the stripped text
     */
    def stripEverything(markup : String) : String = {
        var strippedMarkup = StringEscapeUtils.unescapeHtml(markup)

        strippedMarkup = stripSection(strippedMarkup, "see also")
        strippedMarkup = stripSection(strippedMarkup, "references")
        strippedMarkup = stripSection(strippedMarkup, "further reading")
        strippedMarkup = stripSection(strippedMarkup, "external links")
        //strippedMarkup = stripHeadings(strippedMarkup)
        strippedMarkup = stripMagicWords(strippedMarkup)
        strippedMarkup = stripFormatting(strippedMarkup)
        strippedMarkup = stripBullets(strippedMarkup)
        strippedMarkup = stripHTML(strippedMarkup)
        strippedMarkup = stripExcessNewlines(strippedMarkup)

        strippedMarkup
    }

    /**
     * Strips a string of all markup except bullet points of lists; tries to turn it into plain text
     *
     * @param markup the text to be stripped
     * @return the stripped text
     */
    def stripEverythingButBulletPoints(markup : String) : String = {
        var strippedMarkup = StringEscapeUtils.unescapeHtml(markup)

        strippedMarkup = stripSection(strippedMarkup, "see also")
        strippedMarkup = stripSection(strippedMarkup, "references")
        strippedMarkup = stripSection(strippedMarkup, "further reading")
        strippedMarkup = stripSection(strippedMarkup, "external links")
        strippedMarkup = stripHeadings(strippedMarkup)
        strippedMarkup = stripMagicWords(strippedMarkup)
        strippedMarkup = stripFormatting(strippedMarkup)
        //strippedMarkup = stripBullets(strippedMarkup)
        strippedMarkup = stripHTML(strippedMarkup)
        strippedMarkup = stripExcessNewlines(strippedMarkup)

        strippedMarkup
    }

    /**
     * Removes all section headers.
     *
     * @param markup the text to be stripped
     * @return the stripped markup
     */
    def stripHeadings(markup: String): String = {
        markup.replaceAll("""==?=?[\w\s]+?==?=?""", "")
    }

    /**
     * Removes all sections (both header and content) with the given sectionName
     *
     * @param sectionName the name of the section (case insensitive) to remove.
     * @param markup the markup to be stripped
     * @return the stripped markup
     */
    def stripSection(markup: String, sectionName: String): String = {
        var p: Pattern = Pattern.compile("""(={2,})\s*""" + sectionName + """\s*\1.*?([^=]\1[^=])""", Pattern.CASE_INSENSITIVE + Pattern.DOTALL)
        var m: Matcher = p.matcher(markup)
        var sb: StringBuffer = new StringBuffer
        var lastIndex: Int = 0
        while (m.find) {
            sb.append(markup.substring(lastIndex, m.start))
            sb.append(m.group(2))
            lastIndex = m.end
        }
        sb.append(markup.substring(lastIndex))
        val intermediateMarkup = sb.toString
        p = Pattern.compile("""(={2,})\s*""" + sectionName + """\s*\1\W*.*?\n\n""", Pattern.CASE_INSENSITIVE + Pattern.DOTALL)
        m = p.matcher(intermediateMarkup)
        sb = new StringBuffer
        lastIndex = 0
        while (m.find) {
            sb.append(intermediateMarkup.substring(lastIndex, m.start))
            lastIndex = m.end - 2
        }
        sb.append(intermediateMarkup.substring(lastIndex))
        return sb.toString
    }

    /**
     * Strips all <ref> tags from the given markup; both those that provide links to footnotes, and the footnotes themselves.
     *
     * @param markup the text to be stripped
     * @return the stripped text
     */
    def stripRefs(markup: String): String = {
        // called in stripHTML
        var strippedMarkup: String = markup.replaceAll("""<ref\\\\>""", "")
        strippedMarkup = strippedMarkup.replaceAll("""(?s)<ref>(.*?)</ref>""", "")
        strippedMarkup = strippedMarkup.replaceAll("""(?s)<ref\s(.*?)>(.*?)</ref>""", "")
        return strippedMarkup
    }

    /**
     * Removes special "magic word" (???) syntax, such as __NOTOC__
     *
     * @param markup the text to be stripped
     * @return the stripped markup
     */
    def stripMagicWords(markup: String): String = {
        markup.replaceAll("""\_\_(\p{Upper}+\_\_)""", "")
    }

    /**
     * Strips all wiki formatting, the stuff that makes text bold, italicised, intented, listed, or made into headers.
     *
     * @param markup the text to be stripped
     * @return the stripped markup
     */
    def stripFormatting(markup: String): String = {
        var strippedMarkup: String = markup.replaceAll("""'{2,}""", "")
        strippedMarkup = strippedMarkup.replaceAll("""={2,}""", "")
        strippedMarkup = strippedMarkup.replaceAll("""\n:+""", "\n")
        return strippedMarkup
    }

    def stripMultiPipe(markup: String): String = {
        // used in WikiParagraphSource separately
        markup.replaceAll("""^.*\|""", "")
    }

    /**
     * Strips html comments and tags except for links from the given markup. Text found between tags is not removed.
     *
     * @param markup the text to be stripped
     * @return the stripped text
     */
    def stripHTML(markup: String): String = {
        var strippedMarkup: String = markup.replaceAll("""(?s)\<\!\-\-(.*?)\-\-\>""", "")
        strippedMarkup = stripRefs(strippedMarkup)
        strippedMarkup = strippedMarkup.replaceAll("""<([^>]*?)>""", "")
        return strippedMarkup
    }



    /**
     * Collapses consecutive newlines into at most two newlines.
     * This is provided because stripping out templates and tables often leaves large gaps in the text.
     *
     * @param markup the text to be stripped
     * @return the stripped markup
     */
    def stripExcessNewlines(markup: String): String = {
        markup.replaceAll("""\n{3,}""", "\n\n")
    }

    /**
     * Removes starting bullet points
     *
     * @param markup the text to be stripped
     * @return the stripped markup
     */
    def stripBullets(markup: String): String = {
        markup.replaceAll("""\n\*+[^\w\[]*""", "\n")
    }

}