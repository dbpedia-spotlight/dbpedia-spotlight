package org.apache.lucene.analysis.sk;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.apache.lucene.analysis.util.StemmerUtil.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Light Stemmer for Slovak.
 * <p>
 * Ported to Lucene 3.6.0 the algorithm and code described in:  
 * <i>
 * Slovak stemmer
 * </i>
 * http://vi.ikt.ui.sav.sk/Projekty/Projekty_2008%2F%2F2009/Hana_Pifkov%C3%A1_-_Stemer
 * </p>
 */
public class SlovakStemmer {

    private static ArrayList<String []> pripony = vytvorPripony();
	
	private static String [] ei = new String[] {"e", "i", "iam", "iach", "iami", "í", "ia", "ie", "iu", "ím"};
	
	private static HashMap<String, String> dtnl = vytvorDTNL();
	
	private static HashMap<String, String> dlheKratke = vytvorDlheKratke();
	
	private static String [] cudzieSlovaPredIa = new String[] {"c", "z", "g"};
	
	private static String [] samohlasky = new String[] {"a", "á", "ä", "e", "é", "i", "í", "o", "ó", "u", "ú", "y", "ý", "ô", "ia", "ie", "iu"};
	
	private static String [] lr = new String [] {"r", "ŕ", "l", "ĺ"};
	
	
    /**
     * Stem an input buffer of Slovak text.
     *
     * @param s input buffer
     * @param len length of input buffer
     * @return stem string (which is not necessarily a subset of the input)
     *
     * <p><b>NOTE</b>: Input is expected to be in lowercase,
     * but with diacritical marks</p>
     */
    public String stem(char s[], int len) {
        String in = new String(s);
        
        return zbavSaPripon(in);
        
        //este daco ine?
    }

	private static String zbavSaPripon(String in) {
		
		for(String [] prip : pripony) {
			
			for(String pripona : prip) {
				
				//nasli sme priponu
				if(in.endsWith(pripona)) {
					
					//detenele, ditinili
					if(ei(pripona)) {
						return zmenDTNL(odstranPriponu(in, pripona));
					}
					
					//cudzie -cia, -gia...
					if(pripona.startsWith("i")) {
						if(cudzie(in, pripona)) {
							return odstranPriponu(in, pripona).concat("i");
						}
					}
                    
					//ci nepride k overstemmingu
					if(overstemming(in, pripona))
						return in;
					
					//inak odstranime priponu
					return odstranPriponu(in, pripona);
				}
			}
		}
		
		//konci na er -> peter, sveter....
		if(in.endsWith("er")) {
			return (odstranPriponu(in, "er")).concat("r");
		}
		
		//konci na ok -> sviatok, odpadok....
		if(in.endsWith("ok")) {
			return (odstranPriponu(in, "ok")).concat("k");
		}
		
		//konci na zen -> podobizen, bielizen....
		if(in.endsWith("zeň")) {
			return (odstranPriponu(in, "eň")).concat("ň");
		}
		
		//konci na ol -> kotol....
		if(in.endsWith("ol")) {
			return (odstranPriponu(in, "ol")).concat("l");
		}
		
		//konci na ic -> matematic (matematik, matematici)... (pracovnici vs slnecnic)
		if(in.endsWith("ic")) {
			return (odstranPriponu(in, "c")).concat("k");
		}
		
		//konci na ec -> tanec, obec....
		if(in.endsWith("ec")) {
			return (odstranPriponu(in, "ec")).concat("c");
		}
		
		//konci na um -> studium, stadium....
		if(in.endsWith("um")) {
			return (odstranPriponu(in, "um"));
		}
		
		//genitiv pluralu pre vzory zena, ulica, gazdina, mesto, srdce ???
		return poriesGenitivPluralu(in);
        
	}	
	
	//problem = napriklad pes - psa, den - dna a podobne TODO
	private static boolean overstemming(String in, String pripona) {
		
		//overstemming zrejme vtedy, ked nam ostane koren bez samohlasky / bez l/r v strede slova
		
		String s = odstranPriponu(in, pripona);
		
		for(String samohlaska : samohlasky) {
			if(s.contains(samohlaska))
				return false;
		}
		
		for(String rl : lr) {
			if(s.contains(rl) && !s.endsWith(rl))
				return false;
		}
		
		return true;
	}
	
	//problem = ako rozoznat, ci je to zensky/stredny rod. pr: lama - lam vs. pan - panov TODO
    
	/**
	 *
	 * @param in
	 * @return ak je to genitiv pluralu, vrat spravny tvar, ak nie je, vrat in
	 */
	private static String poriesGenitivPluralu(String in) {
        
		//v poslednej slabike musi byt dlha samohlaska / dlhe r/l
		
		Set<String> dlhe = dlheKratke.keySet();
		for(String dlha : dlhe) {
			
			if(in.contains(dlha)) {
				
				if(poslednaSlabika(in, dlha)) {
					
					in = nahradPosledne(in, dlha, dlheKratke.get(dlha));
					
					break;
				}
			}
			
		}
		
		return in;
	}
    
	
	/**
	 * posledna slabika - ak sa za danym substringom uz nenaxadza uz ziadna samohlaska
	 * @param s string
	 * @param t substring
	 * @return
	 */
	private static boolean poslednaSlabika(String s, String t) {
        
		int pokial = s.lastIndexOf(t);
		String koniec = s.substring(pokial);
		koniec = koniec.substring(t.length());
		
		for(String samohlaska : samohlasky) {
			
			if(koniec.contains(samohlaska))
				return false;
			
		}
		
		return true;
	}
	
	/**
	 * nahradi posledny vyskyt podretazca v retazci inym podretazcom
	 * @param s
	 * @param co
	 * @param cim
	 * @return
	 */
	private static String nahradPosledne(String s, String co, String cim) {
		
		int pokial = s.lastIndexOf(co);
		
		String koniec = s.substring(pokial);
		
		koniec = koniec.substring(co.length());
		
		koniec = cim + koniec;
		
		s = s.substring(0, pokial) + koniec;
		
		return s;
	}
	
	//problem - srdcia TODO
	private static boolean cudzie(String in, String pripona) {
		String s = odstranPriponu(in, pripona);
		
		for(String koncovka : cudzieSlovaPredIa) {
			if(s.endsWith(koncovka))
				return true;
		}
		
		return false;
	}
    
	private static String odstranPriponu(String s, String p) {
		
		if(!s.endsWith(p))
			return s;
		
		return s.substring(0, s.length() - p.length());
	}
	
	//TODO problem - napr sused - sudedia vs. priatel - priatelia
	private static boolean ei(String s1) {
		for(String s2 : ei) {
			if(s1.equals(s2))
				return true;
		}
		return false;
	}	
	
	private static String zmenDTNL(String in) {
		
		Set<String> tvrde = dtnl.keySet();
		
		for(String tvrdy : tvrde) {
			if(in.endsWith(tvrdy)) {
				in = in.substring(0, in.length() - 1);
				in = in.concat(dtnl.get(tvrdy));
			}
		}
		
		return in;
	}
    
	/**
	 * Vytvori zoznam pripon podstatnych mien od najdlhsich po najkratsie tak,
	 * 	ze ak je nejaka pripona obsiahnuta v inej, je kratsia.
	 *
	 * Pripony pre vzory:
	 * 		chlap, hrdina, dub, stroj, hostinsky
	 * 		zena, ulica, dlan, kost, gazdina
	 * 		mesto, srdce, vysvedcenie, dievca (+ holuba)
	 *
	 * @return zoznam pripon
	 */
	private static ArrayList<String[]> vytvorPripony() {
        
		ArrayList<String[]> p = new ArrayList<String[]>();
		
		// od najdlhsich po najkratsie tak, ze ak je nejaka pripona obsiahnuta v inej, je kratsia
		
		//najdlhsie (nie su v ziadnej inej obsiahnute)
		p.add(new String[] {"encami", "atami", "ätami", "iami", "ými", "ovi", "ati", "äti",
            "eniec", "ence", "ie", "aťom", "äťom", "encom", "atám", "ätám", "iam", "ím",
            "ým", "encoch", "atách", "ätách", "iach", "ých", "aťa", "äťa", "ovia", "atá",
            "ätá", "aťu", "äťu", "ému", "iu", "iou", "ov", "at", "ät", "ä", "ého", "ý",
            "y", "ií", "ej", "ú", "é"});
		
		p.add(new String[] {"e", "om", "ami", "ám", "och", "ach", "ách", "ia", "á", "ou", "o", "ii", "í"});
		
		p.add(new String[] {"mi", "a", "u"});
		
		p.add(new String[] {"i"});
		
		return p;
		
	}	
	
	/**
	 * vramcizenskeho a stredneho rodu - ideme riesit genitiv pluralu
	 * @return
	 */
	private static HashMap<String, String> vytvorDlheKratke() {
        
		HashMap<String, String> h = new HashMap<String, String>();
		
		h.put("á", "a");
		h.put("ie", "e");
		h.put("ŕ", "r");
		h.put("ĺ", "l");
		h.put("í", "i");
		h.put("ú", "u");
		h.put("ŕ", "r");
		h.put("ô", "o");
		
		return h;
	}
	
	/**
	 * d -> ď, t -> ť, ...
	 * @return
	 */
	private static HashMap<String, String> vytvorDTNL() {
        
		HashMap<String, String> h = new HashMap<String, String>();
		
		h.put("d", "ď");
		h.put("t", "ť");
		h.put("n", "ň");
		h.put("l", "ľ");
		
		return h;		
	}
 
}
