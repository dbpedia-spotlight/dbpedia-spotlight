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