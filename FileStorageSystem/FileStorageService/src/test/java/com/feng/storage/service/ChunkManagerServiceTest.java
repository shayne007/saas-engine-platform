package com.feng.storage.service;

import com.feng.storage.client.GcsStorageClient;
import com.feng.storage.entity.FileEntity;
import com.feng.storage.repository.ChunkedUploadRepository;
import com.feng.storage.repository.FileRepository;
import com.feng.storage.service.api.ChunkUploadResult;
import com.feng.storage.service.api.ChunkedUploadSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChunkManagerServiceTest {

    @Mock
    private ChunkedUploadRepository chunkedUploadRepository;
    
    @Mock
    private FileRepository fileRepository;
    
    @Mock
    private GcsStorageClient storageClient;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @InjectMocks
    private ChunkManagerService chunkManagerService;
    
    private static final String UPLOAD_ID = "test-upload-id";
    private static final String FILE_NAME = "test-file.txt";
    private static final long TOTAL_SIZE = 10000L;
    private static final int CHUNK_SIZE = 5000;
    private static final String PROJECT_ID = "test-project";
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(storageClient.getDefaultBucket()).thenReturn("test-bucket");
    }
    
    @Test
    void testCreateUploadSession() {
        // Act
        ChunkedUploadSession session = chunkManagerService.createUploadSession(
            UPLOAD_ID, FILE_NAME, TOTAL_SIZE, CHUNK_SIZE, PROJECT_ID);
        
        // Assert
        assertNotNull(session);
        assertEquals(UPLOAD_ID, session.getUploadId());
        assertEquals(FILE_NAME, session.getFileName());
        assertEquals(TOTAL_SIZE, session.getTotalSize());
        assertEquals(CHUNK_SIZE, session.getChunkSize());
        assertEquals(PROJECT_ID, session.getProjectId());
        assertEquals(2, session.getTotalChunks());
        assertNotNull(session.getUploadedChunks());
        assertEquals(0, session.getUploadedChunks().cardinality());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getExpiresAt());
        
        // Verify Redis interaction
        verify(valueOperations).set(
            eq("chunk_session:" + UPLOAD_ID),
            eq(session),
            any(Duration.class)
        );
    }
    
    @Test
    void testGetUploadSession() {
        // Arrange
        ChunkedUploadSession mockSession = createMockSession();
        when(valueOperations.get("chunk_session:" + UPLOAD_ID)).thenReturn(mockSession);
        
        // Act
        ChunkedUploadSession session = chunkManagerService.getUploadSession(UPLOAD_ID);
        
        // Assert
        assertNotNull(session);
        assertEquals(UPLOAD_ID, session.getUploadId());
    }
    
    @Test
    void testGenerateChunkUploadUrl() {
        // Arrange
        ChunkedUploadSession mockSession = createMockSession();
        when(valueOperations.get("chunk_session:" + UPLOAD_ID)).thenReturn(mockSession);
        when(storageClient.generateSignedUploadUrl(anyString(), anyString(), anyString(),
				any(Duration.class)))
            .thenReturn("https://test-signed-url.com");
        
        // Act
        String url = chunkManagerService.generateChunkUploadUrl(UPLOAD_ID, 1);
        
        // Assert
        assertNotNull(url);
        assertEquals("https://test-signed-url.com", url);
    }
    
    @Test
    void testUploadChunk() throws IOException {
        // Arrange
        ChunkedUploadSession mockSession = createMockSession();
        when(valueOperations.get("chunk_session:" + UPLOAD_ID)).thenReturn(mockSession);
        
        MockMultipartFile mockFile = new MockMultipartFile(
            "chunk", "chunk1", "text/plain", "test content".getBytes());
        
        // Act
        ChunkUploadResult result = chunkManagerService.uploadChunk(UPLOAD_ID, 1, mockFile);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getChunkNumber());
        assertFalse(result.isComplete());
        
        // Verify GCS upload was called
        verify(storageClient).uploadFile(
            eq("test-bucket"),
            eq("chunks/" + UPLOAD_ID + "/1"),
            any(),
            eq("text/plain")
        );
        
        // Verify session was updated
        verify(valueOperations).set(
            eq("chunk_session:" + UPLOAD_ID),
            any(ChunkedUploadSession.class),
            any(Duration.class)
        );
    }
    
    @Test
    void testFinalizeUpload() {
        // Arrange
        ChunkedUploadSession mockSession = createMockSession();
        // Set all chunks as uploaded
        BitSet uploadedChunks = new BitSet(2);
        uploadedChunks.set(0, 2);
        mockSession.setUploadedChunks(uploadedChunks);
        
        when(valueOperations.get("chunk_session:" + UPLOAD_ID)).thenReturn(mockSession);
        
        FileEntity mockFileEntity = FileEntity.builder()
            .id(UUID.randomUUID())
            .filename(FILE_NAME)
            .gcsObjectKey("files/test-file.txt")
            .build();
        
        when(fileRepository.save(any(FileEntity.class))).thenReturn(mockFileEntity);
        when(chunkedUploadRepository.save(any())).thenReturn(null);
        when(storageClient.completeMultipartUpload(anyString(), anyString())).thenReturn("files/test-file.txt");
        
        // Act
        FileEntity result = chunkManagerService.finalizeUpload(UPLOAD_ID);
        
        // Assert
        assertNotNull(result);
        assertEquals(mockFileEntity.getId(), result.getId());
        assertEquals(FileEntity.UploadStatus.COMPLETED, result.getUploadStatus());
        
        // Verify GCS combine was called
        verify(storageClient).completeMultipartUpload(eq(UPLOAD_ID), anyString());
        
        // Verify file entity was saved
        verify(fileRepository, times(2)).save(any(FileEntity.class));
    }
    
    private ChunkedUploadSession createMockSession() {
        return ChunkedUploadSession.builder()
            .uploadId(UPLOAD_ID)
            .fileName(FILE_NAME)
            .totalSize(TOTAL_SIZE)
            .chunkSize(CHUNK_SIZE)
            .totalChunks(2)
            .projectId(PROJECT_ID)
            .uploadedChunks(new BitSet(2))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(Duration.ofHours(24)))
            .build();
    }
}