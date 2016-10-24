package org.thoughtcrime.securesms.service;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.util.BitmapUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@TargetApi(23)
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

    if (conversationList.getCount() < 1) {
      conversationList.close();
      return targets;
    }

    if (conversationList.moveToFirst()) {
      do {
        Long threadID = conversationList.getLong(
            conversationList.getColumnIndex(ThreadDatabase.ID));
        String recipientIds = conversationList.getString(
            conversationList.getColumnIndex(ThreadDatabase.RECIPIENT_IDS));
        int distributionType = conversationList.getInt(
            conversationList.getColumnIndex(ThreadDatabase.TYPE));

        Iterator<Recipient> recipientIterator = RecipientFactory.getRecipientsForIds(this, recipientIds, false).iterator();
        while (recipientIterator.hasNext()) {
          Recipient currentRecipient = recipientIterator.next();

          Bundle extras = new Bundle();
          extras.putBoolean("isDirectShare", true);
          extras.putLong(ThreadDatabase.ID, threadID);
          extras.putInt(ThreadDatabase.TYPE, distributionType);

          String displayName = currentRecipient.getName();

          if (displayName == null) {
            displayName = currentRecipient.getNumber();
          }

          Drawable d = currentRecipient.getContactPhoto()
              .asDrawable(this, currentRecipient.getColor().toConversationColor(this), false);
          int largeIconTargetSize = this.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
          Icon targetDisplayIcon = Icon.createWithBitmap(
              BitmapUtil.createFromDrawable(d, largeIconTargetSize, largeIconTargetSize));

          targets.add(
              new ChooserTarget(
                  displayName,
                  targetDisplayIcon,
                  1.0f,
                  componentName,
                  extras
              )
          );

          if (targets.size() >= 8) {
            break;
          }
        }
      } while (conversationList.moveToNext());
    }

    conversationList.close();
    return targets;
  }
}
