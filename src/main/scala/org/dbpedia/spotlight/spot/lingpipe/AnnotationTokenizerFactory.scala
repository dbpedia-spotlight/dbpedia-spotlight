package org.dbpedia.spotlight.spot.lingpipe

import com.aliasi.tokenizer.{RegExTokenizerFactory, ModifyTokenTokenizerFactory}
import java.util.regex.Pattern

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 21.09.2010
 * Time: 15:41:45
 * To change this template use File | Settings | File Templates.
 */


object AnnotationTokenizerFactory
        extends ModifyTokenTokenizerFactory(
            new RegExTokenizerFactory(TokenizerRegEx.BASE_REGEX, Pattern.CASE_INSENSITIVE))