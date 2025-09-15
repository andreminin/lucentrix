const { createApp, ref, onMounted } = Vue;

const app = createApp({
    setup() {
        const uploadProgress = ref(0);
        const processed = ref(0);
        const total = ref(0);
        const uploading = ref(false);
        const scanPath = ref('./documents');
        const pullPath = ref('./documents');
        const documents = ref([]);
        const fileStatus = ref({});
        const clearStatus = ref('');
        const selectedFiles = ref([]);
        const eventSource = ref(null);

        const handleFileSelect = (event) => {
            selectedFiles.value = Array.from(event.target.files);
        };

        const uploadFiles = async () => {
            if (selectedFiles.value.length === 0) {
                alert('Please select at least one file');
                return;
            }

            uploading.value = true;
            processed.value = 0;
            total.value = selectedFiles.value.length;
            uploadProgress.value = 0;
            fileStatus.value = {};

            // Initialize status for all files
            selectedFiles.value.forEach(file => {
                fileStatus.value[file.name] = 'PROCESSING';
            });

            const formData = new FormData();
            selectedFiles.value.forEach(file => {
                formData.append('files', file);
            });

            try {
                // Set up Server-Sent Events listener for progress
                if (eventSource.value) {
                    eventSource.value.close();
                }

                eventSource.value = new EventSource('/api/crawler/progress');
                eventSource.value.onmessage = (event) => {
                    const data = JSON.parse(event.data);
                    processed.value = data.processed;
                    total.value = data.total;
                    uploadProgress.value = total.value > 0 ?
                        (processed.value / total.value) * 100 : 0;
                };

                // Upload files
                const response = await fetch('/api/crawler/push', {
                    method: 'PUT',
                    body: formData
                });

                if (response.ok) {
                    const result = await response.text();
                    console.log('Upload successful:', result);
                } else {
                    console.error('Upload failed:', response.statusText);
                }
            } catch (error) {
                console.error('Error uploading files:', error);
            } finally {
                uploading.value = false;
                if (eventSource.value) {
                    eventSource.value.close();
                    eventSource.value = null;
                }
            }
        };

        const scanDirectory = async () => {
            try {
                const response = await fetch(`/api/crawler/scan?url=${encodeURIComponent(scanPath.value)}`, {
                    method: 'POST'
                });

                if (response.ok) {
                    const result = await response.json();
                    documents.value = result;
                    alert(`Scanned and indexed ${result.length} documents`);
                } else {
                    alert('Error scanning directory');
                }
            } catch (error) {
                console.error('Error scanning directory:', error);
                alert('Error scanning directory');
            }
        };

        const pullMetadata = async () => {
            documents.value = [];

            try {
                // Set up Server-Sent Events listener for document metadata
                if (eventSource.value) {
                    eventSource.value.close();
                }

                eventSource.value = new EventSource(`/api/crawler/pull?url=${encodeURIComponent(pullPath.value)}`);
                eventSource.value.onmessage = (event) => {
                    const data = JSON.parse(event.data);
                    documents.value.push(data.data);
                };

                // Stop listening after 30 seconds
                setTimeout(() => {
                    if (eventSource.value) {
                        eventSource.value.close();
                        eventSource.value = null;
                    }
                }, 30000);
            } catch (error) {
                console.error('Error pulling metadata:', error);
            }
        };

        const clearIndex = async () => {
            try {
                const response = await fetch('/api/crawler/clear', {
                    method: 'DELETE'
                });

                if (response.ok) {
                    clearStatus.value = 'Index cleared successfully';
                    setTimeout(() => {
                        clearStatus.value = '';
                    }, 3000);
                } else {
                    clearStatus.value = 'Error clearing index';
                }
            } catch (error) {
                console.error('Error clearing index:', error);
                clearStatus.value = 'Error clearing index';
            }
        };

        onMounted(() => {
            // Any initialization code
        });

        return {
            uploadProgress,
            processed,
            total,
            uploading,
            scanPath,
            pullPath,
            documents,
            fileStatus,
            clearStatus,
            selectedFiles,
            handleFileSelect,
            uploadFiles,
            scanDirectory,
            pullMetadata,
            clearIndex
        };
    }
});

app.mount('#app');