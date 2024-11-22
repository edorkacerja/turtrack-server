package com.turtrack.server.rabbitmq.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static com.turtrack.server.util.Constants.RabbitMQ.*;

@Service
@Profile("dev")
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange = "turtrack.exchange";

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendToBeScrapedCells(Object message) {
        rabbitTemplate.convertAndSend(exchange, TO_BE_SCRAPED_CELLS_QUEUE, message);
    }

    public void sendToBeScrapedDrAvailability(Object message) {
        rabbitTemplate.convertAndSend(exchange, TO_BE_SCRAPED_DR_AVAILABILITY_QUEUE, message);
    }

    public void sendToBeScrapedVehicleDetails(Object message) {
        rabbitTemplate.convertAndSend(exchange, TO_BE_SCRAPED_VEHICLE_DETAILS_QUEUE, message);
    }

    public void sendScrapedCells(Object message) {
        rabbitTemplate.convertAndSend(exchange, SCRAPED_CELLS_QUEUE, message);
    }

    public void sendScrapedVehicleDetails(Object message) {
        rabbitTemplate.convertAndSend(exchange, SCRAPED_VEHICLE_DETAILS_QUEUE, message);
    }

    public void sendScrapedDrAvailability(Object message) {
        rabbitTemplate.convertAndSend(exchange, SCRAPED_DR_AVAILABILITY_QUEUE, message);
    }

    public void sendScrapedVehicleSkeleton(Object message) {
        rabbitTemplate.convertAndSend(exchange, SCRAPED_VEHICLE_SKELETON_QUEUE, message);
    }

    public void sendDlqCells(Object message) {
        rabbitTemplate.convertAndSend(exchange, DLQ_CELLS_QUEUE, message);
    }

    public void sendDlqDrAvailability(Object message) {
        rabbitTemplate.convertAndSend(exchange, DLQ_DR_AVAILABILITY_QUEUE, message);
    }

    public void sendDlqVehicleDetails(Object message) {
        rabbitTemplate.convertAndSend(exchange, DLQ_VEHICLE_DETAILS_QUEUE, message);
    }
}