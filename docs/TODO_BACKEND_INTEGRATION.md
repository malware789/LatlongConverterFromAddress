# Backend Integration Checklist

This document outlines the specific requirements and steps to transition from Fake APIs to real production endpoints.

## 1. Information Needed from Backend Team
- [ ] **Base URL**: The production/staging root URL.
- [ ] **Fetch Endpoint**: Path for `GET` addresses (e.g., `/v1/tasks/addresses`).
- [ ] **Upload Endpoint**: Path for `POST` geocoded results (e.g., `/v1/tasks/upload`).
- [ ] **Auth Strategy**: (Bearer Token, API Key, etc.).
- [ ] **Pagination Style**: (Cursor-based, Page/Size, or Offset).
- [ ] **Rate Limiting**: Limits per minute/hour and if `Retry-After` headers are supported.
- [ ] **Error Format**: Standard JSON structure for error responses (e.g., `{"code": "...", "message": "..."}`).

## 2. API Contract Specifications

### Fetch Addresses
- **Method**: `GET`
- **Params**: `limit` (int), `cursor` (string, optional)
- **Sample Request**: `/api/v1/addresses?limit=100&cursor=server_id_102`
- **Success Response (200)**: See [fetch_addresses_response_sample.json](api_samples/fetch_addresses_response_sample.json)

### Upload Geocoded Batch
- **Method**: `POST`
- **Header**: `Idempotency-Key` (UUID string)
- **Payload**: See [upload_geocoded_request_sample.json](api_samples/upload_geocoded_request_sample.json)
- **Success Response (200)**: See [upload_geocoded_response_sample.json](api_samples/upload_geocoded_response_sample.json)
- **Note**: The backend must treat the `Idempotency-Key` (batchId) as a unique identifier for the transaction to prevent duplicate data processing.

## 3. Implementation Steps in Android App
1. **Update AppConfig**: 
   - Set `USE_FAKE_API = false`
   - Set `BASE_URL` to the real value.
2. **Update AddressApi.kt**:
   - Update `@GET` and `@POST` paths.
   - Adjust DTO field names in `dto.kt` to match the exact JSON keys.
3. **Configure Auth**:
   - Update `NetworkModule.kt` to add an `Authorization` interceptor to `OkHttpClient`.
   - Implement token refresh logic in `RetrofitAddressRemoteDataSource` if needed.
4. **Refine Mappers**:
   - Update `AddressMappers.kt` if the mapping logic changes (e.g., more fields).
5. **Handle Specific Errors**:
   - Adjust `RetrofitAddressRemoteDataSource.handleResponse` if the backend uses specific HTTP codes for business logic failures.

## 4. Testing Checklist
- [ ] Verify `Start Work` pulls addresses correctly using pagination.
- [ ] Verify `Upload` works and handles network timeouts via retry logic.
- [ ] Verify `Idempotency-Key` prevents duplicate data if the same batch is re-sent.
- [ ] Verify `401 Unauthorized` triggers the auth error flow.
- [ ] Verify `429 Rate Limited` respects the `Retry-After` delay.
- [ ] Verify logs in `MainActivity` show real API status codes and error messages.
