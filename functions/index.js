'use strict'

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/notifications/{user_id}/{notification_id}').onWrite((change, context) => {
  const user_id = context.params.user_id;
  const notification_id = context.params.notification_id;

  console.log('We have a notification to send to: ', user_id);

  // Exit when the data is deleted.
  if (!change.after.exists()) {
    return console.log('A notification has been deleted from the database:', notification_id);
  }

  const fromUser = admin.database().ref(`notifications/${user_id}/${notification_id}`).once('value');
  return fromUser
  .then((fromUserResult) => {

    const from_user_id = fromUserResult.val().from;
    console.log('You have new notification from : ', from_user_id);

    var promises = [];
    var user_id_promise = new Promise(((resolve) => {
      resolve(from_user_id);
    }));

    promises.push(
      admin.database().ref(`users/${from_user_id}/profile_info/username`).once("value")
    );
    promises.push(
      admin.database().ref(`users/${user_id}/device_token`).once("value")
    );
    promises.push(user_id_promise);
      
    return Promise.all(promises);
  })
  .then((res) => {

    const userName = res[0].val();
    const token_id = res[1].val();

    const user_ID = res[2];
    console.log("WE GOT: ", user_ID);

    const payload = {
      notification: {
        title : "New Friend Request",
          body: `${userName} has sent you request`,
          icon: "default",
          click_action: "argdev.io.flingin_TARGET_NOTIFICATION"
      },
      data: {
        userID: user_ID
      }
    };

    return admin.messaging().sendToDevice(token_id, payload);
  })
  .then(() => {
    return console.log('This was the notification Feature');
   })
   .catch((error) => {
     console.log("ERROR:: ", error);
   });
});