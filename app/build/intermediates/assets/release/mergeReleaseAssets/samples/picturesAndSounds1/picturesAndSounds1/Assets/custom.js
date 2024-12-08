/* JavaScript custom code to process ACORNS lesson types for Biblical Greek */

		function Custom()
		{
			var SPECIALS =
			[
				[ ['\u03b1', 0x1f00, 0x1f07, '\u1F00-\u1F07'], ['\u03b1', 0x1f80, 0x1f87, '\u1F80-\u1F87'], // alpha
				  ['\u03b1', 0x1fb2, 0x1fb4, '\u1FB2-\u1FB4'], ['\u03b1', 0x1fb6, 0x1fb7, '\u1FB6-\u1FB7'], 
				  ['\u03b1', 0x1f70, 0x1f71, '\u1F70-\u1F71'] ], 
				
				[ ['\u03b5', 0x1f10, 0x1f15, '\u1F10-\u1F15'], ['\u03b5', 0x1f72, 0x1f73, '\u1F72-\u1F73'] ],  // epsilon

				[ ['\u03b7', 0x1f20, 0x1f27, '\u1F20-\u1F27'], ['\u03b7', 0x1f90, 0x1f97, '\u1F90-\u1F97'],  // eta
		          ['\u03b7', 0x1fc2, 0x1fc4, '\u1FC2-\u1FC4'], ['\u03b7', 0x1fc6, 0x1fc7, '\u1FC6-\u1FC7'], 
				  ['\u03b7', 0x1f74, 0x1f75, '\u1F74-\u1F75']  ],

				[ ['\u03b9', 0x1f30, 0x1f37, '\u1F30-\u1F37'], ['\u03b9', 0x1fd6, 0x1fd6, '\u1FD6-\u1FD7'],  // iota
				  ['\u03b9', 0x1f76, 0x1f77, '\u1F76-\u1F77'] ], 

				[ ['\u03bf', 0x1f40, 0x1f45, '\u1F40-\u1F45'], ['\u03bf', 0x1f78, 0x1f79, '\u1F78-\u1F79'] ], // omnicron

				[ ['\u03c1', 0x1fe4, 0x1fe5, '\u1FE4-\u1FE5'] ], // rho
				
				[ ['\u03c5', 0x1fe6, 0x1fe6, '\u1FE6-\u1FE6'], ['\u03c5', 0x1f50, 0x1f57, '\u1F50-\u1F57'],  // upsilon
				  ['\u03c5', 0x1f7A, 0x1f7B, '\u1F7A-\u1F7B'] ], 

				[ ['\u03c9', 0x1f60, 0x1f67, '\u1F60-\u1F67'], ['\u03c9', 0x1fa0, 0x1fa7, '\u1FA0-\u1FA7'],  // omega
				  ['\u03c9', 0x1ff2, 0x1ff4, '\u1FF0-\u1FF4'], ['\u03c9', 0x1ff6, 0x1ff7, '\u1FF6-\u1FF7'], 
				  ['\u03c9', 0x1f7c, 0x1f7d, '\u1F7C-\u1F7D'] ],

				[ ['\u0391', 0x1f08, 0x1f0f, '\u1F08-\u1F0F'], ['\u0391', 0x1f88, 0x1f8f, '\u1F88-\u1F8F'], //alpha
				  ['\u0391', 0x1fba, 0x1fbc, '\u1FBA-\u1FBC'] ],
				
				[ ['\u0395', 0x1f18, 0x1f1d, '\u1F18-\u1F1D'], ['\u0395', 0x1fc8, 0x1fc9, '\u1FC8-\u1FC9'] ], //epsilon

				[ ['\u0397', 0x1f28, 0x1f2f, '\u1F28-\u1F2F'], ['\u0397', 0x1f98, 0x1f9f, '\u1F98-\u1F9F'],  // eta
				  ['\u0397', 0x1fca, 0x1fcc, '\u1FCA-\u1FCC'] ],

				[ ['\u0399', 0x1f38, 0x1f3f, '\u1F38-\u1F3F'], ['\u0399', 0x1fda, 0x1fdb, '\u1FDA-\u1FDB'] ], // iota
				
				[ ['\u039f', 0x1f48, 0x1f4d, '\u1F48-\u1F4D'], ['\u039f', 0x1ff8, 0x1ff9, '\u1FF8-\u1FF9'] ],  // omnicron

				[ ['\u03a1', 0x1fec, 0x1fec, '\u1FEC-\u1FEC'] ], // rho

				[ ['\u03a5', 0x1f59, 0x1f59, '\u1F59-\u1F59'], ['\u03a5', 0x1f5b, 0x1f5b, '\u1F5B-\u1F5B'], // upsilon
				  ['\u03a5', 0x1f5D, 0x1f5D, '\u1F5D-\u1F5D'], ['\u03a5', 0x1f5f, 0x1f5f, '\u1F5F-\u1F5F'], 
				  ['\u03a5', 0x1fea, 0x1feb, '\u1FEA-\u1FEB'] ],
				
				[ ['\u03a9', 0x1f68, 0x1f6f, '\u1F68-\u1F6F'], ['\u03a9', 0x1fa8, 0x1faf, '\u1FA8-\u1FAF'], // omega
				  ['\u03a9', 0x1ffa, 0x1ffc, '\u1FFA-\u1FFC'] ],				
				  
		   ];
		   
		   this.specialCharacters = function()
		   {
				return SPECIALS;
		   }
		   
			// Correct acute accent discrepancies and use Extended Greek Unicode
			this.convertSpecialChars = function (str)
			{
				if (!str) return;
				
				str = str.replace(/[\u0340-\u0342]/g,"");
				var source  = [0x03AC, 0x03AD, 0x03AE, 0x03AF, 0x03CC, 0x03CD, 0x03CE
							 , 0x0386, 0x0388, 0x0389, 0x038A, 0x038C, 0x038E, 0x038F];
				var dest = [0x1F71, 0x1F73, 0x1F75, 0x1F77, 0x1F79, 0x1F7B, 0x1F7D
							 , 0x1FBB, 0x1FC9, 0x1FCB, 0x1FDB, 0x1FF9, 0x1FDB, 0x1FFB];
				var chars = str.split(''), index, offset;
				for (index=0; index<chars.length; index++)
				{
					offset = source.indexOf(chars[index].charCodeAt(0));
					if (offset>=0) 
						chars[index] = String.fromCharCode(dest[offset]);
				}
				
				var result = chars.join('');
				return result;
			}
		}

