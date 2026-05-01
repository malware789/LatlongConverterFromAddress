package com.watsoo.addressconverter.geocode

import com.google.common.truth.Truth.assertThat
import com.watsoo.addressconverter.config.AppConfig
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class FakeGeocoderTest {

    @Before
    fun setup() {
        AppConfig.FAKE_GEOCODER_TEMP_FAILURE_PERCENT = 0.0f
        AppConfig.FAKE_GEOCODER_PERM_FAILURE_PERCENT = 0.0f
    }

    @Test
    fun `same address returns same coordinates`() = runBlocking {
        val geocoder = FakeGeocoderClient()
        val addr = "1600 Amphitheatre Pkwy"
        
        val result1 = geocoder.geocode(addr)
        val result2 = geocoder.geocode(addr)
        
        assertThat(result1.latitude).isEqualTo(result2.latitude)
        assertThat(result1.longitude).isEqualTo(result2.longitude)
    }

    @Test
    fun `different addresses return different coordinates`() = runBlocking {
        val geocoder = FakeGeocoderClient()
        
        val result1 = geocoder.geocode("Address A")
        val result2 = geocoder.geocode("Address B")
        
        assertThat(result1.latitude).isNotEqualTo(result2.latitude)
    }
}
