package com.turtrack.server.service.turtrack;

import com.turtrack.server.model.manager.Job;
import com.turtrack.server.repository.manager.JobRepository;
import com.turtrack.server.model.turtrack.*;
import com.turtrack.server.repository.turtrack.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("dev")
public class VehicleDetailsService {

    private final VehicleRepository vehicleRepository;
    private final LocationRepository locationRepository;
    private final VehicleDeliveryLocationRepository vehicleDeliveryLocationRepository;
    private final OwnerRepository ownerRepository;
    private final ImageRepository imageRepository;
    private final RatingRepository ratingRepository;
    private final ExtraRepository extraRepository;
    private final JobRepository jobRepository;
    private final DeliveryLocationRepository deliveryLocationRepository;
    private final BadgeRepository badgeRepository; // Add BadgeRepository

    @Transactional
    public void consumeScrapedVehicleDetails(Map<String, Object> message) {
        Long vehicleId = extractLongValue(message, "vehicleId");
        Long jobId = extractLongValue(message, "jobId");

        try {
            Map<String, Object> scrapedData = extractScrapedData(message);
            Vehicle vehicle = processVehicleDetails(scrapedData, vehicleId);
            updateJobProgress(jobId, true);
            log.info("Successfully processed vehicle details for vehicleId: {}", vehicleId);
        } catch (Exception e) {
            log.error("Error processing vehicle details for vehicleId: {}", vehicleId, e);
            updateJobProgress(jobId, false);
        }
    }

    private Map<String, Object> extractScrapedData(Map<String, Object> message) {
        Object scrapedObj = message.get("scraped");
        if (!(scrapedObj instanceof Map)) {
            throw new IllegalArgumentException("scraped data is missing or not a map");
        }
        return (Map<String, Object>) scrapedObj;
    }

    @SuppressWarnings("unchecked")
    private Vehicle processVehicleDetails(Map<String, Object> scrapedData, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByExternalId(vehicleId)
                .orElse(new Vehicle());
        vehicle.setExternalId(vehicleId);
        vehicle = vehicleRepository.save(vehicle);

        Map<String, Object> vehicleData = (Map<String, Object>) scrapedData.get("vehicle");
        if (vehicleData == null) {
            throw new IllegalArgumentException("Vehicle data is missing in scraped data");
        }

        // Update basic vehicle properties
        updateIfChanged(vehicle::setMake, vehicle.getMake(), getStringValue(vehicleData, "make"));
        updateIfChanged(vehicle::setModel, vehicle.getModel(), getStringValue(vehicleData, "model"));
        updateIfChanged(vehicle::setYear, vehicle.getYear(), getIntegerValue(vehicleData, "year"));
        updateIfChanged(vehicle::setTrim, vehicle.getTrim(), getStringValue(vehicleData, "trim"));
        updateIfChanged(vehicle::setType, vehicle.getType(), getStringValue(vehicleData, "type"));
        updateIfChanged(vehicle::setName, vehicle.getName(), getStringValue(vehicleData, "name"));
        updateIfChanged(vehicle::setUrl, vehicle.getUrl(), getStringValue(vehicleData, "url"));
        updateIfChanged(vehicle::setAutomaticTransmission, vehicle.getAutomaticTransmission(), getBooleanValue(vehicleData, "automaticTransmission"));

        // Handle listingCreatedTime
        Long listingCreatedTime = getLongValue(vehicleData, "listingCreatedTime");
        if (listingCreatedTime != null) {
            LocalDateTime createdTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(listingCreatedTime), ZoneId.systemDefault());
            updateIfChanged(vehicle::setListingCreatedTime, vehicle.getListingCreatedTime(), createdTime);
        }

        // Process new fields
        processBasicCarDetails(vehicle, (Map<String, Object>) scrapedData.get("basicCarDetails"));
        processCurrentVehicleProtection(vehicle, (Map<String, Object>) scrapedData.get("currentVehicleProtection"));
        processInstantBookLocationPreferences(vehicle, (Map<String, Object>) scrapedData.get("instantBookLocationPreferences"));

        // Process badges
        processBadges(vehicle, (List<Map<String, Object>>) scrapedData.get("badges"));

        // Process nested objects
        processRegistration(vehicle, (Map<String, Object>) vehicleData.get("registration"));
        List<Map<String, Object>> imagesData = (List<Map<String, Object>>) scrapedData.get("images");
        for(Map<String,Object> imageData: imagesData) {
            processVehicleImage(vehicle, imageData);
        }
        processVehicleDeliveryLocations(vehicle, (List<Map<String, Object>>) scrapedData.get("vehicleDeliveryLocations"));

        // Process other data from scrapedData
        processLocation(vehicle, (Map<String, Object>) scrapedData.get("location"));
        processOwner(vehicle, (Map<String, Object>) scrapedData.get("owner"));
        processRatings(vehicle, (Map<String, Object>) scrapedData.get("ratings"));
        processExtras(vehicle, scrapedData);

        // Process rate information
        Map<String, Object> rate = (Map<String, Object>) scrapedData.get("rate");
        if (rate != null) {
            updateIfChanged(vehicle::setAverageDailyPrice, vehicle.getAverageDailyPrice(), getDoubleValue(rate, "averageDailyPrice"));
            updateDistance(vehicle::setDailyDistance, vehicle.getDailyDistance(), (Map<String, Object>) rate.get("dailyDistance"));
            updateDistance(vehicle::setWeeklyDistance, vehicle.getWeeklyDistance(), (Map<String, Object>) rate.get("weeklyDistance"));
            updateDistance(vehicle::setMonthlyDistance, vehicle.getMonthlyDistance(), (Map<String, Object>) rate.get("monthlyDistance"));
            updateIfChanged(vehicle::setWeeklyDiscountPercentage, vehicle.getWeeklyDiscountPercentage(), getIntegerValue(rate, "weeklyDiscountPercentage"));
            updateIfChanged(vehicle::setMonthlyDiscountPercentage, vehicle.getMonthlyDiscountPercentage(), getIntegerValue(rate, "monthlyDiscountPercentage"));
            updateExcessFeePerDistance(vehicle, (Map<String, Object>) rate.get("excessFeePerDistance"));
        }

        // Process other fields
        updateIfChanged(vehicle::setColor, vehicle.getColor(), getStringValue(scrapedData, "color"));
        updateIfChanged(vehicle::setDescription, vehicle.getDescription(), getStringValue(scrapedData, "description"));
        updateIfChanged(vehicle::setGuidelines, vehicle.getGuidelines(), getStringValue(scrapedData, "guidelines"));
        updateIfChanged(vehicle::setNumberOfFavorites, vehicle.getNumberOfFavorites(), getIntegerValue(scrapedData, "numberOfFavorites"));
        updateIfChanged(vehicle::setNumberOfReviews, vehicle.getNumberOfReviews(), getIntegerValue(scrapedData, "numberOfReviews"));
        updateIfChanged(vehicle::setNumberOfRentals, vehicle.getNumberOfRentals(), getIntegerValue(scrapedData, "numberOfRentals"));
        updateIfChanged(vehicle::setTripCount, vehicle.getTripCount(), getIntegerValue(scrapedData, "tripCount"));
        updateIfChanged(vehicle::setMinimumAgeInYearsToRent, vehicle.getMinimumAgeInYearsToRent(), getIntegerValue(scrapedData, "minimumAgeInYearsToRent"));
        updateIfChanged(vehicle::setFrequentlyBooked, vehicle.getFrequentlyBooked(), getBooleanValue(scrapedData, "frequentlyBooked"));
        updateIfChanged(vehicle::setHighValueVehicle, vehicle.getHighValueVehicle(), getBooleanValue(scrapedData, "highValueVehicle"));
        updateIfChanged(vehicle::setTuroGo, vehicle.getTuroGo(), getBooleanValue(scrapedData, "turoGo"));
        updateIfChanged(vehicle::setVehicleStatus, vehicle.getVehicleStatus(), getStringValue(scrapedData, "vehicleStatus"));
        updateIfChanged(vehicle::setVehicleValueType, vehicle.getVehicleValueType(), getStringValue(scrapedData, "vehicleValueType"));
        updateIfChanged(vehicle::setListingDeleted, vehicle.getListingDeleted(), getBooleanValue(scrapedData, "listingDeleted"));
        updateIfChanged(vehicle::setListingEnabled, vehicle.getListingEnabled(), getBooleanValue(scrapedData, "listingEnabled"));

        vehicle.setDetailLastUpdated(LocalDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    private void processBasicCarDetails(Vehicle vehicle, Map<String, Object> basicCarDetails) {
        if (basicCarDetails == null) return;

        updateIfChanged(vehicle::setAverageFuelEconomy, vehicle.getAverageFuelEconomy(), getDoubleValue(basicCarDetails, "averageFuelEconomy"));
        updateIfChanged(vehicle::setCityFuelEconomy, vehicle.getCityFuelEconomy(), getIntegerValue(basicCarDetails, "cityFuelEconomy"));
        updateIfChanged(vehicle::setHighwayFuelEconomy, vehicle.getHighwayFuelEconomy(), getIntegerValue(basicCarDetails, "highwayFuelEconomy"));
        updateIfChanged(vehicle::setNumberOfDoors, vehicle.getNumberOfDoors(), getIntegerValue(basicCarDetails, "numberOfDoors"));
        updateIfChanged(vehicle::setNumberOfSeats, vehicle.getNumberOfSeats(), getIntegerValue(basicCarDetails, "numberOfSeats"));

        Map<String, Object> fuelType = (Map<String, Object>) basicCarDetails.get("fuelType");
        if (fuelType != null) {
            updateIfChanged(vehicle::setFuelType, vehicle.getFuelType(), getStringValue(fuelType, "label"));
        }

        Map<String, Object> fuelGrade = (Map<String, Object>) basicCarDetails.get("fuelGrade");
        if (fuelGrade != null) {
            updateIfChanged(vehicle::setFuelGrade, vehicle.getFuelGrade(), getStringValue(fuelGrade, "label"));
        }

        updateIfChanged(vehicle::setFuelTypeAndGradeLabel, vehicle.getFuelTypeAndGradeLabel(), getStringValue(basicCarDetails, "fuelTypeAndGradeLabel"));
        updateIfChanged(vehicle::setFuelUnit, vehicle.getFuelUnit(), getStringValue(basicCarDetails, "fuelUnit"));
        updateIfChanged(vehicle::setFuelUnitLabel, vehicle.getFuelUnitLabel(), getStringValue(basicCarDetails, "fuelUnitLabel"));
    }

    private void processCurrentVehicleProtection(Vehicle vehicle, Map<String, Object> protectionData) {
        if (protectionData == null) return;

        updateIfChanged(vehicle::setInsuranceProviderDisplayName, vehicle.getInsuranceProviderDisplayName(), getStringValue(protectionData, "insuranceProviderDisplayName"));
        updateIfChanged(vehicle::setInsuranceProviderDescription, vehicle.getInsuranceProviderDescription(), getStringValue(protectionData, "insuranceProviderDescription"));
        updateIfChanged(vehicle::setVehicleProtectionLevel, vehicle.getVehicleProtectionLevel(), getStringValue(protectionData, "vehicleProtectionLevel"));
        updateIfChanged(vehicle::setHostTakeRate, vehicle.getHostTakeRate(), getDoubleValue(protectionData, "hostTakeRate"));
    }

    private void processInstantBookLocationPreferences(Vehicle vehicle, Map<String, Object> instantBookData) {
        if (instantBookData == null) return;

        updateIfChanged(vehicle::setInstantBookAirportLocationEnabled, vehicle.getInstantBookAirportLocationEnabled(), getBooleanValue(instantBookData, "airportLocationEnabled"));
        updateIfChanged(vehicle::setInstantBookCustomLocationEnabled, vehicle.getInstantBookCustomLocationEnabled(), getBooleanValue(instantBookData, "customLocationEnabled"));
        updateIfChanged(vehicle::setInstantBookHomeLocationEnabled, vehicle.getInstantBookHomeLocationEnabled(), getBooleanValue(instantBookData, "homeLocationEnabled"));
        updateIfChanged(vehicle::setInstantBookPoiLocationEnabled, vehicle.getInstantBookPoiLocationEnabled(), getBooleanValue(instantBookData, "poiLocationEnabled"));
    }

    @SuppressWarnings("unchecked")
    private void processBadges(Vehicle vehicle, List<Map<String, Object>> badgesData) {
        if (badgesData == null || badgesData.isEmpty()) return;

        Set<Badge> updatedBadges = new HashSet<>();

        for (Map<String, Object> badgeData : badgesData) {
            Long externalId = getLongValue(badgeData, "id");
            if (externalId == null) {
                throw new IllegalArgumentException("Badge ID is missing in badge data");
            }

            Badge badge = badgeRepository.findByExternalId(externalId)
                    .orElse(new Badge());
            badge.setExternalId(externalId);

            updateIfChanged(badge::setLabel, badge.getLabel(), getStringValue(badgeData, "label"));
            updateIfChanged(badge::setValue, badge.getValue(), getStringValue(badgeData, "value"));

            badge = badgeRepository.save(badge);
            updatedBadges.add(badge);
        }

        // Update vehicle's badges
        vehicle.setBadges(updatedBadges);
    }

    protected void processVehicleDeliveryLocations(Vehicle vehicle, List<Map<String, Object>> deliveryLocationsData) {
        if (deliveryLocationsData == null) return;

        Set<Long> processedIds = new HashSet<>();

        for (Map<String, Object> data : deliveryLocationsData) {
            Long vehicleDeliveryLocationId = getLongValue(data, "vehicleDeliveryLocationId");
            if (vehicleDeliveryLocationId == null) continue;

            Map<String, Object> deliveryLocationData = (Map<String, Object>) data.get("deliveryLocation");
            if (deliveryLocationData == null) continue;

            String placeId = getStringValue(deliveryLocationData, "placeId");
            DeliveryLocation deliveryLocation = getOrCreateDeliveryLocation(deliveryLocationData);

            VehicleDeliveryLocation vehicleDeliveryLocation = vehicle.getVehicleDeliveryLocations().stream()
                    .filter(vdl -> vdl.getExternalId().equals(vehicleDeliveryLocationId))
                    .findFirst()
                    .orElseGet(() -> createVehicleDeliveryLocation(vehicle, deliveryLocation, vehicleDeliveryLocationId));

            updateVehicleDeliveryLocationFields(vehicleDeliveryLocation, data);
            processedIds.add(vehicleDeliveryLocationId);

            if (!vehicle.getVehicleDeliveryLocations().contains(vehicleDeliveryLocation)) {
                vehicle.getVehicleDeliveryLocations().add(vehicleDeliveryLocation);
            }
        }

        // Remove vehicle delivery locations that are no longer present
        vehicle.getVehicleDeliveryLocations().removeIf(vdl -> !processedIds.contains(vdl.getExternalId()));
    }

    private DeliveryLocation getOrCreateDeliveryLocation(Map<String, Object> data) {
        String externalId = getStringValue(data, "placeId");
        return deliveryLocationRepository.findByExternalId(externalId)
                .orElseGet(() -> createDeliveryLocation(data));
    }

    private DeliveryLocation createDeliveryLocation(Map<String, Object> data) {
        DeliveryLocation deliveryLocation = new DeliveryLocation();
        deliveryLocation.setExternalId(getStringValue(data, "placeId"));
        updateDeliveryLocationFields(deliveryLocation, data);
        return deliveryLocationRepository.save(deliveryLocation);
    }

    private void updateDeliveryLocationFields(DeliveryLocation deliveryLocation, Map<String, Object> data) {
        updateIfChanged(deliveryLocation::setFormattedAddress, deliveryLocation.getFormattedAddress(), getStringValue(data, "formattedAddress"));
        updateIfChanged(deliveryLocation::setLatitude, deliveryLocation.getLatitude(), getDoubleValue(data, "latitude"));
        updateIfChanged(deliveryLocation::setLongitude, deliveryLocation.getLongitude(), getDoubleValue(data, "longitude"));
        updateIfChanged(deliveryLocation::setName, deliveryLocation.getName(), getStringValue(data, "name"));
        updateIfChanged(deliveryLocation::setOperational, deliveryLocation.getOperational(), getBooleanValue(data, "operational"));
        updateIfChanged(deliveryLocation::setType, deliveryLocation.getType(), getStringValue(data, "type"));
        updateIfChanged(deliveryLocation::setValetAvailable, deliveryLocation.getValetAvailable(), getBooleanValue(data, "valetAvailable"));
        updateIfChanged(deliveryLocation::setBanner, deliveryLocation.getBanner(), getStringValue(data, "banner"));
    }

    private void updateVehicleDeliveryLocationFields(VehicleDeliveryLocation vehicleDeliveryLocation, Map<String, Object> data) {
        updateIfChanged(vehicleDeliveryLocation::setEnabled, vehicleDeliveryLocation.getEnabled(), getBooleanValue(data, "enabled"));
        updateIfChanged(vehicleDeliveryLocation::setValet, vehicleDeliveryLocation.getValet(), getBooleanValue(data, "valet"));
        updateIfChanged(vehicleDeliveryLocation::setInstructions, vehicleDeliveryLocation.getInstructions(), getStringValue(data, "instructions"));

        Map<String, Object> feeData = (Map<String, Object>) data.get("fee");
        if (feeData != null) {
            VehicleDeliveryLocation.Fee fee = vehicleDeliveryLocation.getFee() != null ? vehicleDeliveryLocation.getFee() : new VehicleDeliveryLocation.Fee();
            updateIfChanged(fee::setAmount, fee.getAmount(), getDoubleValue(feeData, "amount"));
            updateIfChanged(fee::setCurrencyCode, fee.getCurrencyCode(), getStringValue(feeData, "currencyCode"));
            vehicleDeliveryLocation.setFee(fee);
        }

        updateCheckInMethods(vehicleDeliveryLocation, data);
    }

    private void updateCheckInMethods(VehicleDeliveryLocation vehicleDeliveryLocation, Map<String, Object> data) {
        Map<String, Object> checkInMethodData = (Map<String, Object>) data.get("checkInMethod");
        if (checkInMethodData != null) {
            VehicleDeliveryLocation.CheckInMethod checkInMethod = new VehicleDeliveryLocation.CheckInMethod();
            checkInMethod.setCheckInMethod(getStringValue(checkInMethodData, "checkInMethod"));
            checkInMethod.setDescription(getStringValue(checkInMethodData, "description"));
            checkInMethod.setTitle(getStringValue(checkInMethodData, "title"));
            vehicleDeliveryLocation.setCheckInMethod(checkInMethod);
        }

        Map<String, Object> deliveryLocationData = (Map<String, Object>) data.get("deliveryLocation");
        if (deliveryLocationData != null) {
            List<Map<String, Object>> validNonValetCheckInMethods = (List<Map<String, Object>>) deliveryLocationData.get("validNonValetCheckInMethods");
            List<Map<String, Object>> validValetCheckInMethods = (List<Map<String, Object>>) deliveryLocationData.get("validValetCheckInMethods");

            vehicleDeliveryLocation.setValidNonValetCheckInMethods(createCheckInMethods(validNonValetCheckInMethods));
            vehicleDeliveryLocation.setValidValetCheckInMethods(createCheckInMethods(validValetCheckInMethods));
        }
    }

    private VehicleDeliveryLocation createVehicleDeliveryLocation(Vehicle vehicle, DeliveryLocation deliveryLocation, Long vehicleDeliveryLocationId) {
        VehicleDeliveryLocation vehicleDeliveryLocation = new VehicleDeliveryLocation();
        vehicleDeliveryLocation.setExternalId(vehicleDeliveryLocationId);
        vehicleDeliveryLocation.setVehicle(vehicle);
        vehicleDeliveryLocation.setDeliveryLocation(deliveryLocation);
        return vehicleDeliveryLocation;
    }

    private List<VehicleDeliveryLocation.CheckInMethod> createCheckInMethods(List<Map<String, Object>> checkInMethodsData) {
        if (checkInMethodsData == null) return new ArrayList<>();

        return checkInMethodsData.stream()
                .map(this::createCheckInMethod)
                .collect(Collectors.toList());
    }

    private VehicleDeliveryLocation.CheckInMethod createCheckInMethod(Map<String, Object> data) {
        VehicleDeliveryLocation.CheckInMethod checkInMethod = new VehicleDeliveryLocation.CheckInMethod();
        checkInMethod.setCheckInMethod(getStringValue(data, "checkInMethod"));
        checkInMethod.setDescription(getStringValue(data, "description"));
        checkInMethod.setTitle(getStringValue(data, "title"));
        return checkInMethod;
    }

    private void updateDistance(java.util.function.Consumer<Vehicle.Distance> setter, Vehicle.Distance currentDistance, Map<String, Object> distanceData) {
        if (distanceData == null) return;

        Vehicle.Distance newDistance = new Vehicle.Distance();
        newDistance.setScalar(getIntegerValue(distanceData, "scalar"));
        newDistance.setUnit(getStringValue(distanceData, "unit"));
        newDistance.setUnlimited(getBooleanValue(distanceData, "unlimited"));

        if (currentDistance == null || !currentDistance.equals(newDistance)) {
            setter.accept(newDistance);
        }
    }

    private void updateExcessFeePerDistance(Vehicle vehicle, Map<String, Object> excessFeeData) {
        if (excessFeeData == null) return;

        updateIfChanged(vehicle::setExcessFeePerDistance, vehicle.getExcessFeePerDistance(), getDoubleValue(excessFeeData, "amount"));
        updateIfChanged(vehicle::setExcessFeePerDistanceCurrencyCode, vehicle.getExcessFeePerDistanceCurrencyCode(), getStringValue(excessFeeData, "currencyCode"));
    }

    private void processLocation(Vehicle vehicle, Map<String, Object> locationData) {
        if (locationData == null) return;

        Long externalLocationId = extractLongValue(locationData, "id");
        if (externalLocationId == null) {
            throw new IllegalArgumentException("Location ID is missing in location data");
        }

        Location location = locationRepository.findByExternalId(externalLocationId).orElse(new Location());
        location.setExternalId(externalLocationId);

        updateIfChanged(location::setAddress, location.getAddress(), getStringValue(locationData, "address"));
        updateIfChanged(location::setCity, location.getCity(), getStringValue(locationData, "city"));
        updateIfChanged(location::setState, location.getState(), getStringValue(locationData, "state"));
        updateIfChanged(location::setCountry, location.getCountry(), getStringValue(locationData, "country"));
        updateIfChanged(location::setLatitude, location.getLatitude(), getDoubleValue(locationData, "latitude"));
        updateIfChanged(location::setLongitude, location.getLongitude(), getDoubleValue(locationData, "longitude"));
        updateIfChanged(location::setTimeZone, location.getTimeZone(), getStringValue(locationData, "timeZone"));

        locationRepository.save(location);
        vehicle.setLocation(location);
    }

    private void processOwner(Vehicle vehicle, Map<String, Object> ownerData) {
        if (ownerData == null) return;

        Long externalId = getLongValue(ownerData, "id");
        if (externalId == null) {
            throw new IllegalArgumentException("Owner ID is missing in owner data");
        }

        Owner owner = ownerRepository.findByExternalId(externalId).orElse(new Owner());
        owner.setExternalId(externalId);
        updateIfChanged(owner::setFirstName, owner.getFirstName(), getStringValue(ownerData, "firstName"));
        updateIfChanged(owner::setLastName, owner.getLastName(), getStringValue(ownerData, "lastName"));
        updateIfChanged(owner::setIsAllStarHost, owner.getIsAllStarHost(), getBooleanValue(ownerData, "allStarHost"));
        updateIfChanged(owner::setIsProHost, owner.getIsProHost(), getBooleanValue(ownerData, "proHost"));
        updateIfChanged(owner::setUrl, owner.getUrl(), getStringValue(ownerData, "url"));

        processOwnerImage(owner, (Map<String, Object>) ownerData.get("image"));

        ownerRepository.save(owner);
        vehicle.setOwner(owner);
    }

    private void processOwnerImage(Owner owner, Map<String, Object> imageData) {
        if (imageData == null) return;

        Long externalId = getLongValue(imageData, "id");
        String originalUrl = getStringValue(imageData, "originalImageUrl");

        if (originalUrl == null) return; // We need at least the URL to process the image

        Image ownerImage = owner.getImage();

        boolean shouldCreateNewImage = ownerImage == null ||
                (externalId != null && !externalId.equals(ownerImage.getExternalId())) ||
                (externalId == null && !originalUrl.equals(ownerImage.getOriginalUrl()));

        if (shouldCreateNewImage) {
            // No existing image, or IDs don't match, or URLs don't match when ID is null
            ownerImage = new Image();
            ownerImage.setOwner(owner);
            owner.setImage(ownerImage);
        }

        // Update image fields
        ownerImage.setExternalId(externalId); // This might be null, and that's okay
        ownerImage.setOriginalUrl(originalUrl);
        ownerImage.setResizableUrlTemplate(getStringValue(imageData, "resizableUrlTemplate"));
        ownerImage.setIsPrimary(true);

        // Ensure bidirectional relationship
        if (ownerImage.getOwner() == null) {
            ownerImage.setOwner(owner);
        }
    }

    private void processVehicleImage(Vehicle vehicle, Map<String, Object> imageData) {
        if (imageData == null) return;

        Long externalId = getLongValue(imageData, "id");
        if (externalId == null) return;

        Image vehicleImage = vehicle.getImages().stream()
                .filter(image -> externalId.equals(image.getExternalId()))
                .findFirst()
                .orElseGet(() -> {
                    Image newImage = new Image();
                    newImage.setExternalId(externalId);
                    newImage.setVehicle(vehicle);
                    vehicle.getImages().add(newImage);
                    return newImage;
                });

        // Update image fields
        vehicleImage.setOriginalUrl(getStringValue(imageData, "originalImageUrl"));
        vehicleImage.setResizableUrlTemplate(getStringValue(imageData, "resizableUrlTemplate"));
        vehicleImage.setIsPrimary(true);

        // Ensure bidirectional relationship
        if (vehicleImage.getVehicle() == null) {
            vehicleImage.setVehicle(vehicle);
        }
    }

    public Image saveOrUpdateImage(Image image) {
        if (image.getId() != null) {
            Image existingImage = imageRepository.findById(image.getId()).orElse(null);
            if (existingImage != null) {
                // Update fields without changing the ID
                updateImageFields(existingImage, image);
                return imageRepository.save(existingImage);
            }
        }
        // For new images or images without ID
        return imageRepository.save(image);
    }

    private void updateImageFields(Image existingImage, Image newImage) {
        existingImage.setOriginalUrl(newImage.getOriginalUrl());
        existingImage.setIsPrimary(newImage.getIsPrimary());
        existingImage.setResizableUrlTemplate(newImage.getResizableUrlTemplate());
        existingImage.setVerified(newImage.getVerified());
        existingImage.setVehicle(newImage.getVehicle());
        existingImage.setOwner(newImage.getOwner());
    }

    private void processRegistration(Vehicle vehicle, Map<String, Object> registrationData) {
        if (registrationData != null) {
            updateIfChanged(vehicle::setRegistrationState, vehicle.getRegistrationState(), getStringValue(registrationData, "state"));
        }
    }

    @SuppressWarnings("unchecked")
    private void processRatings(Vehicle vehicle, Map<String, Object> ratingsData) {
        if (ratingsData == null) return;

        Rating rating = vehicle.getRatings().stream()
                .filter(r -> r.getVehicle().getId().equals(vehicle.getId()))
                .findFirst()
                .orElse(new Rating());

        updateIfChanged(rating::setOwnerOverall, rating.getOwnerOverall(), getDoubleValue(ratingsData, "ownerOverall"));
        updateIfChanged(rating::setRatingToHundredth, rating.getRatingToHundredth(), getDoubleValue(ratingsData, "ratingToHundredth"));

        Map<String, Object> histogram = (Map<String, Object>) ratingsData.get("histogram");
        if (histogram != null) {
            updateIfChanged(rating::setRatingsCount, rating.getRatingsCount(), getIntegerValue(histogram, "ratingsCount"));

            List<Map<String, Object>> buckets = (List<Map<String, Object>>) histogram.get("buckets");
            if (buckets != null) {
                for (Map<String, Object> bucket : buckets) {
                    String category = (String) bucket.get("category");
                    Double averageRating = getDoubleValue(bucket, "averageRating");

                    switch (category) {
                        case "CLEANLINESS":
                            updateIfChanged(rating::setCleanliness, rating.getCleanliness(), averageRating);
                            break;
                        case "MAINTENANCE":
                            updateIfChanged(rating::setMaintenance, rating.getMaintenance(), averageRating);
                            break;
                        case "COMMUNICATION":
                            updateIfChanged(rating::setCommunication, rating.getCommunication(), averageRating);
                            break;
                        case "CONVENIENCE":
                            updateIfChanged(rating::setConvenience, rating.getConvenience(), averageRating);
                            break;
                        case "LISTING_ACCURACY":
                            updateIfChanged(rating::setAccuracy, rating.getAccuracy(), averageRating);
                            break;
                    }
                }
            }
        }

        rating.setVehicle(vehicle);

        if (!vehicle.getRatings().contains(rating)) {
            vehicle.getRatings().add(rating);
        }
    }

    @SuppressWarnings("unchecked")
    private void processExtras(Vehicle vehicle, Map<String, Object> scrapedData) {
        Map<String, Object> extrasMap = (Map<String, Object>) scrapedData.get("extras");
        if (extrasMap == null) return;

        List<Map<String, Object>> extrasList = (List<Map<String, Object>>) extrasMap.get("extras");
        if (extrasList == null || extrasList.isEmpty()) return;

        Set<Long> updatedExternalIds = new HashSet<>();

        for (Map<String, Object> extraData : extrasList) {
            Long externalId = getLongValue(extraData, "extraId");
            if (externalId == null) {
                throw new IllegalArgumentException("Extra ID is missing in extra data");
            }

            Extra extra = vehicle.getExtras().stream()
                    .filter(e -> e.getExternalId().equals(externalId))
                    .findFirst()
                    .orElseGet(() -> {
                        Extra newExtra = extraRepository.findByExternalId(externalId)
                                .orElse(new Extra());
                        newExtra.setExternalId(externalId);
                        newExtra.setVehicle(vehicle);
                        vehicle.getExtras().add(newExtra);
                        return newExtra;
                    });

            updateIfChanged(extra::setDescription, extra.getDescription(), getStringValue(extraData, "description"));
            updateIfChanged(extra::setEnabled, extra.getEnabled(), getBooleanValue(extraData, "enabled"));
            updateIfChanged(extra::setExtraPricingType, extra.getExtraPricingType(), getStringValue(extraData, "extraPricingType"));
            updateIfChanged(extra::setQuantity, extra.getQuantity(), getIntegerValue(extraData, "quantity"));

            Map<String, Object> extraType = (Map<String, Object>) extraData.get("extraType");
            if (extraType != null) {
                updateIfChanged(extra::setExtraType, extra.getExtraType(), getStringValue(extraType, "value"));
                Map<String, Object> category = (Map<String, Object>) extraType.get("category");
                if (category != null) {
                    updateIfChanged(extra::setExtraCategory, extra.getExtraCategory(), getStringValue(category, "value"));
                }
            }

            Map<String, Object> priceWithCurrency = (Map<String, Object>) extraData.get("priceWithCurrency");
            if (priceWithCurrency != null) {
                updateIfChanged(extra::setPrice, extra.getPrice(), getDoubleValue(priceWithCurrency, "amount"));
                updateIfChanged(extra::setCurrencyCode, extra.getCurrencyCode(), getStringValue(priceWithCurrency, "currencyCode"));
            }

            extraRepository.save(extra);
            updatedExternalIds.add(externalId);
        }

        // Remove extras that are no longer associated with the vehicle
        vehicle.getExtras().removeIf(extra -> !updatedExternalIds.contains(extra.getExternalId()));
    }

    private void updateJobProgress(Long jobId, boolean success) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with id: " + jobId));

        if (success) {
            job.setCompletedItems(job.getCompletedItems() + 1);
        } else {
            job.setFailedItems(job.getFailedItems() + 1);
        }

        int processedItems = job.getCompletedItems() + job.getFailedItems();
        job.setPercentCompleted((double) processedItems / job.getTotalItems() * 100);

        if (processedItems >= job.getTotalItems()) {
            job.setStatus(Job.JobStatus.FINISHED);
            job.setFinishedAt(LocalDateTime.now());
        }

        jobRepository.save(job);
    }

    private <T> void updateIfChanged(java.util.function.Consumer<T> setter, T currentValue, T newValue) {
        if (newValue != null && !newValue.equals(currentValue)) {
            setter.accept(newValue);
        }
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? convertToInteger(value) : null;
    }

    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? convertToDouble(value) : null;
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? Boolean.valueOf(value.toString()) : null;
    }

    private Long extractLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is missing in data");
        }
        return parseLong(value, key);
    }

    private Long parseLong(Object value, String fieldName) {
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for " + fieldName + ": " + value, e);
        }
        throw new IllegalArgumentException("Cannot parse " + fieldName + " from: " + value);
    }

    private Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Integer");
    }

    private Double convertToDouble(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Double");
    }
}
