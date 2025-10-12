package com.feng.storage.service.api;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileQueryResponse {

	private List<FileMetadata> files;

	private Long total;
}


