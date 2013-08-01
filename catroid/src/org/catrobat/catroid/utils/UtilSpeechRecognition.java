/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.utils;

import android.os.Bundle;

import org.catrobat.catroid.speechrecognition.RecognizerCallback;
import org.catrobat.catroid.stage.StageActivity;

public class UtilSpeechRecognition {
	private static StageActivity currentRunningStage = null;
	private static String lastAnswer = "";

	public static void setStageActivity(StageActivity currentStage) {
		currentRunningStage = currentStage;
	}

	public static void askUserViaIntent(String question, final RecognizerCallback originCallback) {
		currentRunningStage.askForSpeechInput(question, new RecognizerCallback() {

			@Override
			public void onRecognizerResult(int resultCode, Bundle resultBundle) {
				if (resultCode == RESULT_OK) {
					lastAnswer = resultBundle.getStringArrayList(BUNDLE_RESULT_MATCHES).toString();
				} else {
					lastAnswer = "";
				}
				originCallback.onRecognizerResult(resultCode, resultBundle);
			}

			@Override
			public void onRecognizerError(Bundle errorBundle) {
				lastAnswer = "";
				originCallback.onRecognizerError(errorBundle);
			}
		});
	}

	public static String getLastAnswer() {
		return lastAnswer;
	}
}
