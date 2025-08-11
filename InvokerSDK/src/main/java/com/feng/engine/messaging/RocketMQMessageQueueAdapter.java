package com.feng.engine.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
@ConditionalOnProperty(name = "universal-mq.provider", havingValue = "rocketmq")
public class RocketMQMessageQueueAdapter extends AbstractMessageQueueAdapter {
    private DefaultMQProducer producer;
    private final Map<String, DefaultMQPushConsumer> consumers;
    private final RocketMQMessageConverter messageConverter;

    @Override
    public void send(UniversalMessage message) {
        Message rocketMessage = messageConverter.toRocketMQMessage(message);

        try {
            // Handle different send modes
            if (message.isBroadcast()) {
                // Use broadcast mode
                rocketMessage.setTopic(message.getTopic() + "_BROADCAST");
                producer.send(rocketMessage);
            } else if (message.isTransactional()) {
                // Use transaction message
                sendTransactional(message, rocketMessage);
            } else {
                // Regular send
                SendResult result = producer.send(rocketMessage);
                handleSendSuccess(message, result);
            }
        } catch (Exception e) {
            handleSendFailure(message, e);
        }
    }

    @Override
    public void subscribe(String topic, MessageHandler handler) {
        try {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
            consumer.setConsumerGroup(getConsumerGroup());
            consumer.setNamesrvAddr(getNamesrvAddr());

            // Subscribe to topic
            consumer.subscribe(topic, "*"); // Subscribe to all tags

            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> messages,
                    ConsumeConcurrentlyContext context) {

                    for (MessageExt messageExt : messages) {
                        try {
                            UniversalMessage message = messageConverter.fromRocketMQMessage(messageExt);
                            handler.handle(message);
                        } catch (Exception e) {
                            log.error("Failed to process message", e);
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            consumer.start();
            consumers.put(topic, consumer);

        } catch (MQClientException e) {
            throw new MessageQueueException("Failed to create RocketMQ consumer", e);
        }
    }

    @Override
    protected boolean supportsPartitioning() { return true; }

    @Override
    protected boolean supportsTransactions() { return true; }

    @Override
    protected boolean supportsMessageRetention() { return true; }

    @Override
    protected boolean supportsBroadcast() { return true; }

    // RocketMQ-specific: Native broadcast support
    public void subscribeBroadcast(String topic, MessageHandler handler) {
        try {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
            consumer.setConsumerGroup(getConsumerGroup() + "_BROADCAST");
            consumer.setMessageModel(MessageModel.BROADCASTING); // Broadcast mode
            consumer.setNamesrvAddr(getNamesrvAddr());

            consumer.subscribe(topic, "*");
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> messages,
                    ConsumeConcurrentlyContext context) {

                    messages.forEach(messageExt -> {
                        UniversalMessage message = messageConverter.fromRocketMQMessage(messageExt);
                        handler.handle(message);
                    });

                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            consumer.start();
        } catch (MQClientException e) {
            throw new MessageQueueException("Failed to create broadcast consumer", e);
        }
    }

    // Transaction message support
    public void sendTransactional(UniversalMessage message, Message rocketMessage) {
        TransactionMQProducer transactionProducer = getTransactionProducer();

        TransactionSendResult result = transactionProducer.sendMessageInTransaction(
            rocketMessage,
            message // Local transaction executor argument
        );

        if (result.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
            handleSendSuccess(message, result.getSendResult());
        } else {
            handleSendFailure(message, new Exception("Transaction failed: " +
                result.getLocalTransactionState()));
        }
    }
}

