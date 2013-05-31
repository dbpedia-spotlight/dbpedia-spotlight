/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */
package org.dbpedia.spotlight.spot;

/**
 * Model class used by {@link OpenNLPNGramSpotter} to store a sequence of tokens
 *
 * @author Rohana Rajapakse (GOSS Interactive Limited) - implemented the class
 */
public class NGram {
	
	String textform = null;
	int start = -1;
	int end = -1;
//	String context = null; //might not need this
	
	public NGram() {

	}

	public NGram(String txtfrm, int start, int end) {
		this.textform = txtfrm;
		this.start = start;
		this.end = end;
	}
	
	public boolean equals(NGram ngrm2) {
		boolean ret = false;
		if (this.textform.equalsIgnoreCase(ngrm2.getTextform()) && this.start == ngrm2.getStart() && this.end == ngrm2.getEnd()) {
			ret = true;
		}
		return ret;
	}

	public String getTextform() {
		return textform;
	}

	public void setTextform(String textform) {
		this.textform = textform;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

//	public String getContext() {
//		return context;
//	}
//
//	public void setContext(String context) {
//		this.context = context;
//	}
	
}
