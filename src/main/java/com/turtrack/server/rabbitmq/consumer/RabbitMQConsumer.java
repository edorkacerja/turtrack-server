package com.turtrack.server.rabbitmq.consumer;

import com.turtrack.server.service.manager.CellService;
import com.turtrack.server.service.turtrack.DailyRateAndAvailabilityService;
import com.turtrack.server.service.turtrack.VehicleDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.turtrack.server.util.Constants.RabbitMQ.*;

@Service
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final VehicleDetailsService vehicleDetailsService;
    private final DailyRateAndAvailabilityService dailyRateAndAvailabilityService;
    private final CellService cellService;

    @RabbitListener(queues = SCRAPED_CELLS_QUEUE)
    public void consumeScrapedCells(Map<String, Object> message) {
        System.out.println("Received scraped cell message: " + message);
        try {
            cellService.processCell(message);
        } catch (Exception e) {
            // TODO: do not acknowledge message.
        }
    }

//    @RabbitListener(queues = SCRAPED_VEHICLE_DETAILS_QUEUE)
//    public void consumeVehicles(Map<String, Object> message) {
//        System.out.println("Received vehicle message: " + message);
//        try {
//            vehicleDetailsService.consumeScrapedVehicleDetails(message);
//        } catch (Exception e) {
//            System.out.println("EXCEPTION: " + message);
//        }
//    }

    @RabbitListener(queues = SCRAPED_DR_AVAILABILITY_QUEUE)
    public void consumePricing(Map<String, Object> message) {
        System.out.println("Received pricing message: " + message);
        dailyRateAndAvailabilityService.consumeScrapedDailyRates(message);
    }

    @RabbitListener(queues = DLQ_DR_AVAILABILITY_QUEUE)
    public void consumeDLQPricing(Map<String, Object> message) {
        System.out.println("Received DLQ pricing message: " + message);
        dailyRateAndAvailabilityService.consumeScrapedDailyRates(message);
    }
}
