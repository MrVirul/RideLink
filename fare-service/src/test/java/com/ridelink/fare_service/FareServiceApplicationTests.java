package com.ridelink.fare_service;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.service.FareService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class FareServiceApplicationTests {

	@Test
	void calculatesFareEstimateFromRideAndDistance() {
		FareEstimateRequest request = new FareEstimateRequest();
		request.setRideId(101L);
		request.setDistanceKm(5);

		FareEstimateResponse response = new FareService().calculateEstimate(request);

		assertEquals(101L, response.getRideId());
		assertEquals(5.0, response.getDistanceKm());
		assertEquals(700.0, response.getEstimatedFare());
	}

	@Test
	void contextLoads() {
	}

}
