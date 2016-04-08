/*
 * This file is part of OpenAstexViewer.
 *
 * OpenAstexViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAstexViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAstexViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package astex;

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 29-12-99 mjh
 *	created
 */
import java.io.*;

/**
 * Implements a FilenameFilter that can take a list of
 * accepted extensions in the construtor.
 */
public class UniversalFilenameFilter implements FilenameFilter {
	/** The file extensions we will accept. */
	String acceptedExtensions[] = null;

	/** Constructor that specifies the accepted extensions. */
	public UniversalFilenameFilter(String extensions[]){
		setExtensions(extensions);
	}

	/** Set the accepted file name extensions. */
	public void setExtensions(String extensions[]){
		acceptedExtensions = extensions;
	}

	/** Tell whether we want to accept this file. */
	public boolean accept(File dir, String name){
		String lowerCaseName = name.toLowerCase();
		
		for(int i = 0; i < acceptedExtensions.length; i++){
			String lowerCaseExtension =
				acceptedExtensions[i].toLowerCase();
			if(lowerCaseName.endsWith(lowerCaseExtension)){
				
				return true;
			}
		}
		
		return false;
	}
}
