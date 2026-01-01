const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendChatNotification = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const snap = event.data;
    if (!snap || !snap.exists) {
      console.log("❌ No document data");
      return;
    }

    const message = snap.data();
    console.log("📩 New message:", message);

    if (!message) {
      console.log("❌ Empty message object");
      return;
    }

    // Skip self-messages
    if (message.senderId === message.receiverId) {
      console.log("🙅 Self message - skipping");
      return;
    }

    const receiverId = message.receiverId;
    const senderId = message.senderId;
    const text = message.message || "New message";
    const chatId = event.params.chatId;

    try {
      // 1) Get receiver (notification target)
      const receiverDoc = await admin
        .firestore()
        .collection("users")
        .doc(receiverId)
        .get();

      if (!receiverDoc.exists) {
        console.log("❌ Receiver doc not found:", receiverId);
        return;
      }

      const receiverData = receiverDoc.data();
      const token = receiverData && receiverData.fcmToken;

      if (!token) {
        console.log("❌ No fcmToken for receiver:", receiverId);
        return;
      }

      // 2) Get sender (for title)
      const senderDoc = await admin
        .firestore()
        .collection("users")
        .doc(senderId)
        .get();

      let senderName = "Someone";
      if (senderDoc.exists && senderDoc.data().name) {
        senderName = senderDoc.data().name;
      }

      const payload = {
        token: token,
        notification: {
          title: senderName,
          body: text,
        },
        data: {
          chatId: chatId,
          type: "message",
        },
      };

      console.log(
        "🚀 Sending push to",
        receiverId,
        "token:",
        token.substring(0, 15) + "..."
      );

      const response = await admin.messaging().send(payload);
      console.log("✅ Push sent:", response);
    } catch (err) {
      console.error("❌ Error in sendChatNotification:", err);
    }
  }
);
