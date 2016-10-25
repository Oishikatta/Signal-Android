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
import org.thoughtcrime.securesms.contacts.avatars.ContactColors;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhotoFactory;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.util.BitmapUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
      do {
        Long threadID = conversationList.getLong(
            conversationList.getColumnIndex(ThreadDatabase.ID));
        String recipientIds = conversationList.getString(
            conversationList.getColumnIndex(ThreadDatabase.RECIPIENT_IDS));
        int distributionType = conversationList.getInt(
            conversationList.getColumnIndex(ThreadDatabase.TYPE));

        Bundle extras = new Bundle();
        extras.putBoolean("isDirectShare", true);
        extras.putLong(ThreadDatabase.ID, threadID);
        extras.putInt(ThreadDatabase.TYPE, distributionType);

        StringBuilder displayName = new StringBuilder();
        ContactPhoto contactPhoto = ContactPhotoFactory.getDefaultContactPhoto(null);
        MaterialColor contactPhotoColor = ContactColors.UNKNOWN_COLOR;

        Iterator<Recipient> recipientIterator = RecipientFactory.getRecipientsForIds(this, recipientIds, false).iterator();

        for (int recipientIdx = 0; recipientIterator.hasNext(); recipientIdx++) {
          Recipient currentRecipient = recipientIterator.next();
          String recipientDisplayName = currentRecipient.getName();

          if (recipientDisplayName == null) {
            recipientDisplayName = currentRecipient.getNumber();
          }

          displayName.append(recipientDisplayName);

          if (recipientIterator.hasNext()) {
            displayName.append(", ");
          }

          if (recipientIdx == 0 && !recipientIterator.hasNext()) {
            contactPhoto = currentRecipient.getContactPhoto();
            contactPhotoColor = currentRecipient.getColor();
          } else if (recipientIdx == 1) {
            contactPhoto = ContactPhotoFactory.getDefaultGroupPhoto();
            contactPhotoColor = currentRecipient.getColor();
          }
        }

        targets.add(
            new ChooserTarget(
                displayName,
                getIconFromContactPhoto(contactPhoto, contactPhotoColor),
                1.0f,
                componentName,
                extras
            )
        );

        if (targets.size() >= 8) {
          break;
        }
      } while (conversationList.moveToNext());
    }

    conversationList.close();
    return targets;
  }

  private Icon getIconFromContactPhoto (ContactPhoto contactPhoto, MaterialColor contactPhotoColor) {
    Drawable d = contactPhoto.asDrawable(this, contactPhotoColor.toConversationColor(this));
    int largeIconTargetSize = this.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);

    return Icon.createWithBitmap(
            BitmapUtil.createFromDrawable(d, largeIconTargetSize, largeIconTargetSize)
        );
  }
}
