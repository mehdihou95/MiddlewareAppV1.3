import React, { useState, useCallback } from 'react';
import { processedFileService } from '../../services/inbound_config/processedFileService';
import { ProcessedFile } from '../../services/inbound_config/types';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

interface FileUploadProps {
  clientId: number;
  interfaceId: number;
  interfaceType: string;
  onUploadSuccess: (processedFile: ProcessedFile) => void;
  onUploadError: (error: string) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({
  clientId,
  interfaceId,
  interfaceType,
  onUploadSuccess,
  onUploadError,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  const validateFileSize = (file: File): boolean => {
    return file.size <= MAX_FILE_SIZE;
  };

  const validateFileType = (file: File): boolean => {
    if (interfaceType === 'ASN') {
      return file.type === 'text/xml' || file.name.toLowerCase().endsWith('.xml');
    }
    // For other interface types
    const fileName = file.name.toLowerCase();
    return file.type === 'text/xml' || 
           file.type === 'application/json' || 
           file.type === 'text/csv' ||
           !!fileName.match(/\.(xml|json|csv|edi)$/);
  };

  const handleFileChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      // Validate file size (10MB max)
      if (!validateFileSize(selectedFile)) {
        setError('File size exceeds 10MB limit');
        return;
      }

      // Validate file type
      if (!validateFileType(selectedFile)) {
        setError(`Invalid file type for ${interfaceType} interface`);
        return;
      }

      setFile(selectedFile);
      setError('');
    }
  }, [interfaceType]);

  const handleUpload = useCallback(async () => {
    if (!file) {
      setError('Please select a file to upload');
      return;
    }

    try {
      setIsUploading(true);
      setError('');
      
      const processedFile = await processedFileService.uploadFile(file, interfaceId);
      onUploadSuccess(processedFile);
      
      // Reset form
      setFile(null);
      setUploadProgress(0);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred during upload';
      setError(errorMessage);
      onUploadError(errorMessage);
    } finally {
      setIsUploading(false);
    }
  }, [file, interfaceId, onUploadSuccess, onUploadError]);

  return (
    <div className="file-upload-container">
      <div className="file-input-wrapper">
        <input
          type="file"
          accept=".xml"
          onChange={handleFileChange}
          disabled={isUploading}
          className="file-input"
        />
        <label className="file-label">
          {file ? file.name : 'Choose XML file'}
        </label>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {isUploading && (
        <div className="progress-bar">
          <div 
            className="progress-fill"
            style={{ width: `${uploadProgress}%` }}
          />
        </div>
      )}

      <button
        onClick={handleUpload}
        disabled={!file || isUploading}
        className={`upload-button ${isUploading ? 'uploading' : ''}`}
      >
        {isUploading ? 'Uploading...' : 'Upload'}
      </button>

      <style>{`
        .file-upload-container {
          padding: 20px;
          border: 2px dashed #ccc;
          border-radius: 8px;
          text-align: center;
        }

        .file-input-wrapper {
          position: relative;
          margin-bottom: 15px;
        }

        .file-input {
          position: absolute;
          width: 100%;
          height: 100%;
          opacity: 0;
          cursor: pointer;
        }

        .file-label {
          display: inline-block;
          padding: 10px 20px;
          background-color: #f0f0f0;
          border-radius: 4px;
          cursor: pointer;
        }

        .error-message {
          color: #dc3545;
          margin: 10px 0;
        }

        .progress-bar {
          width: 100%;
          height: 4px;
          background-color: #f0f0f0;
          border-radius: 2px;
          margin: 10px 0;
          overflow: hidden;
        }

        .progress-fill {
          height: 100%;
          background-color: #007bff;
          transition: width 0.3s ease;
        }

        .upload-button {
          padding: 10px 20px;
          background-color: #007bff;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          transition: background-color 0.3s ease;
        }

        .upload-button:disabled {
          background-color: #ccc;
          cursor: not-allowed;
        }

        .upload-button.uploading {
          background-color: #6c757d;
        }
      `}</style>
    </div>
  );
};

export default FileUpload; 