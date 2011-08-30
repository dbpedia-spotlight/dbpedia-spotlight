			/**
			 * Copyright (c) 2009 Mark S. Kolich
			 * http://mark.kolich.com
			 *
			 * Permission is hereby granted, free of charge, to any person
			 * obtaining a copy of this software and associated documentation
			 * files (the "Software"), to deal in the Software without
			 * restriction, including without limitation the rights to use,
			 * copy, modify, merge, publish, distribute, sublicense, and/or sell
			 * copies of the Software, and to permit persons to whom the
			 * Software is furnished to do so, subject to the following
			 * conditions:
			 *
			 * The above copyright notice and this permission notice shall be
			 * included in all copies or substantial portions of the Software.
			 *
			 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
			 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
			 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
			 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
			 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
			 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
			 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
			 * OTHER DEALINGS IN THE SOFTWARE.
			*/
			
			if(!window.Kolich){
				Kolich = {};
			}
			
			Kolich.Selector = {};
			// getSelected() was borrowed from CodeToad at
			// http://www.codetoad.com/javascript_get_selected_text.asp
			Kolich.Selector.getSelected = function(){
				var t = '';
				if(window.getSelection){
					t = window.getSelection();
				}else if(document.getSelection){
					t = document.getSelection();
				}else if(document.selection){
					t = document.selection.createRange().text;
				}
				return t;
			}

			
			


