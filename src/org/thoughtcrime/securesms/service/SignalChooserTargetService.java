/**
 * Copyright (C) 2016 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.securesms.service;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.util.Log;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.color.MaterialColor;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.util.BitmapUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to respond to direct share (chooser target) requests from the system
 *
 * @author Corey Hunter
 */
@TargetApi(Build.VERSION_CODES.M)
public class SignalChooserTargetService extends ChooserTargetService {
  private static final String TAG = SignalChooserTargetService.class.getSimpleName();

  @Override
  public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                 IntentFilter matchedFilter) {
    ComponentName componentName = new ComponentName(getPackageName(),
        ShareActivity.class.getCanonicalName());
    ArrayList<ChooserTarget> targets = new ArrayList<>();

    ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(this);
    Cursor conversationList = threadDatabase.getConversationList();

    if (conversationList.moveToFirst()) {
      ThreadDatabase.Reader threadReader = threadDatabase.readerFor(conversationList, null);
      for (ThreadRecord thread = threadReader.getCurrent(); thread != null; thread = threadReader.getNext()) {
        targets.add(getChooserTarget(thread, componentName));
        if (targets.size() >= 8) {
          break;
        }
      }
    } else {
      Log.w(TAG, "No open conversations, returning empty direct share target list.");
    }

    conversationList.close();
    return targets;
  }

  private ChooserTarget getChooserTarget(ThreadRecord thread, ComponentName componentName) {
    Bundle extras = new Bundle();
    extras.putBoolean("isDirectShare", true);
    extras.putLong(ThreadDatabase.ID, thread.getThreadId());
    extras.putInt(ThreadDatabase.TYPE, thread.getDistributionType());
    Recipients recipients = thread.getRecipients();

    return new ChooserTarget(
        recipients.toShortString(),
        getIconFromContactPhoto(recipients.getContactPhoto(), recipients.getColor()),
        1.0f,
        componentName,
        extras
    );
  }

  private Icon getIconFromContactPhoto (ContactPhoto contactPhoto, MaterialColor contactPhotoColor) {
    Drawable d = contactPhoto.asDrawable(this, contactPhotoColor.toConversationColor(this));
    int largeIconTargetSize = this.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);

    return Icon.createWithBitmap(
            BitmapUtil.createFromDrawable(d, largeIconTargetSize, largeIconTargetSize)
        );
  }
}
