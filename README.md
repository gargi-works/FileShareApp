A simple Android application for wireless file sharing between nearby devices without using the internet.  
Inspired by apps like SHAREit, this project demonstrates peer-to-peer communication using sockets and local networking.

---

## Overview

FileShareApp allows users to transfer files between devices using a local network.  
Both the sender and receiver must have the application installed.

The file transfer is completely offline and works only when both devices are connected through a shared network.

---

## Application Flow

### Home Screen

This is the initial screen visible to the user when the app is opened.

- Provides two options:
  - Send File
  - Receive File
- Simple and minimal interface for easy navigation

<img width="300" alt="image" src="https://github.com/user-attachments/assets/9bef464c-5490-445d-b8f5-96891a1673a5" />

---

## Sender Flow

### Step 1: Select File

- User navigates to **Send File**
- Clicks on **Select File**
- Android file picker opens
- User selects any file
- Selected file name is displayed on screen

<img width="300" alt="image" src="https://github.com/user-attachments/assets/ee87fb83-51b5-4fd7-ae72-5827941fbd51" />
<img width="300" alt="image" src="https://github.com/user-attachments/assets/b3033d8a-2b27-47ff-9c6c-c44ab9785c16" />



---

### Step 2: Waiting for Devices

- App starts scanning for nearby devices
- If receiver is not active:
<img width="300" alt="image" src="https://github.com/user-attachments/assets/ecaf2f05-ff8b-4d22-a70c-366c01ce4ac2" />

---

### Step 3: Device Detected

- When receiver starts, device name appears
  
- Sender sees available devices dynamically

---

### Step 4: Send File

- User clicks on detected device
- File is sent to receiver

---

## Receiver Flow

### Step 1: Open Receive Screen

- User navigates to **Receive File**
- Option to open hotspot settings (if required)

<img width="300" alt="image" src="https://github.com/user-attachments/assets/1357d756-8b89-4653-9842-051b8bd2b8a5" />


---

### Step 2: Start Receiving

- User clicks **Start Receiving**
- Server starts and waits for connection
- Status updates to:

<img width="300" alt="image" src="https://github.com/user-attachments/assets/789d9ff2-256e-4871-94eb-106b6096e90e" />


---

### Step 3: Device Broadcast

- Receiver broadcasts its presence
- Sender devices can now detect this device
<img width="300" alt="image" src="https://github.com/user-attachments/assets/5af8d4af-2f77-4c96-bb90-2ee4eee342fb" />


---

### Step 4: Incoming Request

- Sender connects to receiver
- Receiver reads file details:
  - File name
  - File size
  - File type

---

### Step 5: Confirmation Dialog

- Receiver gets a prompt:
  - Accept or Reject file

<img width="300" alt="image" src="https://github.com/user-attachments/assets/828d6173-9a80-4d3c-a998-82dfbc30b9c0" />


---

### Step 6: File Saving

- If accepted:
  - File is received
  - Stored using MediaStore
  - Saved in appropriate directory:
    - Images → Pictures/FileShareApp
    - Audio → Music/FileShareApp
    - Video → Movies/FileShareApp
    - Others → Download/FileShareApp
   
  <img width="300" alt="image" src="https://github.com/user-attachments/assets/cd72c76e-d9e9-44f0-87ef-c62ef5b79377" />
  <img width="300" alt="image" src="https://github.com/user-attachments/assets/4f74a70a-7795-43cf-99e0-cb2526dc6cce" />



---

### Step 7: Auto Open

- File opens automatically after download
- Uses correct MIME type

---

## Network Requirement

Since the app uses local networking, both devices must be connected in one of the following ways:

---

### 1. Same WiFi Network (Primary Method)

- Both devices are connected to the same WiFi network
- Most stable and reliable method

---

### 2. Hotspot-Based Connection (Device Dependent)

- Receiver turns ON hotspot
- Sender connects to hotspot

Important Notes:

- Slower than WiFi
- Device dependent
- May not work on all phones
- Not reliable in this project due to device restrictions

---

### 3. WiFi Direct (Future Scope)

- Direct device-to-device connection
- No manual hotspot needed
- Similar to SHAREit

Status: Not implemented yet

---

## File Transfer Capability

The application supports multiple file types:

- Images (jpg, jpeg, png)
- Audio (mp3, wav)
- Video (mp4)
- Documents (pdf, txt, docx)
- Compressed files (zip)

---

## Technical Working

### Device Discovery (UDP)

- Receiver sends broadcast messages
- Sender listens on port 8888
- Devices are discovered dynamically

---

### File Transfer (TCP)

- Sender connects using:
  Socket(ip, 9999)
- Reliable communication using TCP

---

### Metadata Exchange

Before file transfer, sender sends:
fileName | fileSize | fileType

Receiver reads this before downloading.

---

### File Transfer Process

1. Metadata sent first  
2. Small delay introduced  
3. File data streamed using:
   inputStream.copyTo(outputStream)

---

### File Storage

- Uses MediaStore API
- Determines file type using MIME
- Saves files in correct folders

---

## Key Characteristics

- Works completely offline
- Uses local network communication
- Requires both devices to be in proximity
- No internet required
- Performance depends on network stability

---

## Note

Due to variations in Android devices, hotspot-based transfer may not work consistently.

For best results, use the same WiFi network.

---

## Team Members

This project was developed as part of a Mobile Computing mini project by:

- Gargi Shringare  
- Asma Sayed  
- Isha Samant  
- Smrutishree  

---

## Short Description

Android app for fast wireless file sharing between nearby devices using sockets and local networking.
