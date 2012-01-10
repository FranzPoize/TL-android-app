/*
 * Copyright 2010, 2011 Ali Piccioni
 *
 * This program is distributed under the terms of the GNU General Public License
 *
 *  This file is part of Team Liquid Android App.
 *
 *  Team Liquid Android App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Team Liquid Android App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Team Liquid Android App.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.thoughtmetric.tl;

import java.nio.CharBuffer;

public class HtmlTools {
	private static int size = 8;
	private static CharBuffer to = CharBuffer.allocate(size*1024);
	
	public static String unescapeHtml(String s) {
		char from[] = s.toCharArray();
		if (s.length() > to.length()){
			size *= 2;
			to = CharBuffer.allocate(size * 1024);
		}
		to.clear();
		int i = 0;
		int idState = 0;
		
		for(i = 0; i < from.length; i++) {
			switch(idState) {
			case 0: // 
				switch( from[i] ) {
					case '&':	// &
						idState = 1;
						break;
					default:
						idState = 0;
						to.put(from[i]);
				} break;
			case 1: // &
				switch( from[i] ) {
					case 'a':	// &a
					case 'A':
						idState = 2;
						break;
					case 'g':	// &g
					case 'G':
						idState = 10;
						break;
					case 'l':	// &l
					case 'L':
						idState = 13;
						break;
					case 'q':	// &q
					case 'Q':
						idState = 16;
						break;
					default:
						idState = 0;
						to.put("&");
						to.put(from[i]);
						
						
				} break;
			case 2: // &a
				switch( from[i] ) {
					case 'm':	// &am
					case 'M':
						idState = 3;
						break;
					case 'p':	// &ap
					case 'P':
						idState = 6;
						break;
					default:
						idState = 0;
						to.put("&a");
						to.put(from[i]);
						
				} break;
			case 3: // &am
				switch( from[i] ) {
					case 'p':	// &amp
					case 'P':
						idState = 4;
						break;
					default:
						idState = 0;
						to.put("&am");
						to.put(from[i]);
						
				} break;
			case 4: // &amp
				switch( from[i] ) {
					case ';':	// &amp;
						to.put("&");
						idState = 0;
						break;
					default:
						idState = 0;
						to.put("&amp");
						to.put(from[i]);
						
				} break;
			case 5: // &amp;
				switch( from[i] ) {
					default:
						idState = 0;
						to.put("&amp;");
						to.put(from[i]);
						
				} break;
			case 6: // &ap
				switch( from[i] ) {
					case 'o':	// &apo
					case 'O':
						idState = 7;
						break;
					default:
						idState = 0;
						to.put("&ap");
						to.put(from[i]);
						
				} break;
			case 7: // &apo
				switch( from[i] ) {
					case 's':	// &apos
					case 'S':
						idState = 8;
						break;
					default:
						idState = 0;
						to.put("&apo");
						to.put(from[i]);
						
				} break;
			case 8: // &apos
				switch( from[i] ) {
					case ';':	// &apos;
						to.put("'");
						idState = 0;
						break;
					default:
						idState = 0;
						to.put("&apos");
						to.put(from[i]);
						
				} break;
			case 9: // &apos;
				switch( from[i] ) {
					default:
						idState = 0;
						to.put("&apos;");
						to.put(from[i]);
						
				} break;
			case 10: // &g
				switch( from[i] ) {
					case 't':	// &gt
					case 'T':
						idState = 11;
						break;
					default:
						idState = 0;
						to.put("&g");
						to.put(from[i]);
						
				} break;
			case 11: // &gt
				switch( from[i] ) {
					case ';':	// &gt;
						to.put(">");
						idState = 0;
						break;
					default:
						idState = 0;
						to.put("&gt");
						to.put(from[i]);
						
				} break;
			case 12: // &gt;
				switch( from[i] ) {
					default:
						idState = 0;
						to.put("&gt;");
						to.put(from[i]);
						
				} break;
			case 13: // &l
				switch( from[i] ) {
					case 't':	// &lt
					case 'T':
						idState = 14;
						break;
					default:
						idState = 0;
						to.put("&l");
						to.put(from[i]);
						
				} break;
			case 14: // &lt
				switch( from[i] ) {
					case ';':	// &lt;
						to.put("<");
						idState = 0;
						break;
					default:
						idState = 0;
						to.put("&lt");
						to.put(from[i]);
						
				} break;
			case 15: // &lt;
				switch( from[i] ) {
					default:
						idState = 0;
						to.put("&lt;");
						to.put(from[i]);
						
				} break;
			case 16: // &q
				switch( from[i] ) {
					case 'u':	// &qu
					case 'U':
						idState = 17;
						break;
					default:
						idState = 0;
						to.put("&q");
						to.put(from[i]);			
				} break;
			case 17: // &qu
				switch( from[i] ) {
					case 'o':	// &quo
					case 'O':
						idState = 18;
						break;
					default:
						idState = 0;
						to.put("&qu");
						to.put(from[i]);
						
				} break;
			case 18: // &quo
				switch( from[i] ) {
					case 't':	// &quot
					case 'T':
						idState = 19;
						break;
					default:
						idState = 0;
						to.put("&quo");
						to.put(from[i]);
						
				} break;
			case 19: // &quot
				switch( from[i] ) {
					case ';':	// &quot;
						to.put("\"");
						idState = 0;
						break;
					default:
						idState = 0;
						to.put("&quot");
						to.put(from[i]);
						
				} break;
			case 20: // &quot;
				switch( from[i] ) {
					default:
						idState = 0;
						to.put("&quot;");
						to.put(from[i]);
				} break;

			}
		}
		return String.valueOf(to.array(), 0, to.position());
	}
}
