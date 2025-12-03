<div align="center">
  <img src="bm25.png" alt="Best Matching 25-th iteration" width="500">

# BM25 Search library

**A fast and lightweight BM25-based library to search through the corpus of provided files implemented in JAVA**
</div>


---

## Description

From [Wikipedia](https://en.wikipedia.org/wiki/Okapi_BM25):
> **BM25
** is a bag-of-words retrieval function that ranks a set of documents based on the query terms appearing in each document, regardless of their proximity within the document.


---

## Table of Contents

1. [Description](#description)
2. [Overview](#overview)
3. [Protocol](#protocol)
4. [Message](#message)
5. [Example](#example)
6. [Docker Instructions](#docker-instructions)
7. [Advantage](#advantage)
8. [Use cases](#use-cases)
9. [Authors](#authors)

## Overview

Retrivium is a TCP-based document search engine that implements the BM25 ranking algorithm. The protocol enables clients to:

- List indexed documents
- Search for relevant documents using queries
- Retrieve document contents
- Upload new documents to the server

The protocol uses a simple text-based request-response model with line-delimited messages.

## Protocol

The "Retrivium" protocol is a text transport protocol. It uses the TCP transport protocol to ensure the reliability of data transmission. The default port it uses is the port number 6433.

Every message must be encoded in UTF-8 and delimited by a newline character (\n). The messages are treated as text messages.

The initial connection must be established by the client.

Once the TCP connection is established, the client can send commands to the server.

The server's role is to respond to each command sent by the user.

If the server does not recognize the command, it sends an error message to the client.

The error must specify which condition has not been met.

On an unknown message, the server must send an error to the client.

The client can send multiple commands during 1 session. Client can close connection with QUIT command or by close the socket.

## Message

The client sends text-based commands to the server. Each command is terminated by a line feed character.

### LIST

Lists all documents currently indexed on the server.

#### Request

```
LIST
```

#### Response

- `FILES <list>` if the server has indexed documents
- `NOTHING_INDEXED` if no documents are available

Example for document exist:

```
> LIST
There are 3 documents presented on the server:
file2.txt
file3.txt
file1.txt
```

Example for none indexed

```
Server has no documents to search through
```

### QUERY

Finds the top k most relevant documents using BM25 ranking.

#### Request

```
QUERY <k> <query>
```

- `<k> `: number of documents to return
- `<query>` : search terms

#### Response

- `RELEVANT <FILE1 FILE2 ...>` : at least one result
- `NOTHING_RELEVANT` : no document matches
- `NOTHING_INDEXED` : index is empty
- `INVALID <message>` : malformed query

Example output for relevant results :

```
> QUERY 2 dog
There is 1 relevant documents to your query (starting from most relevant):
file2.txt
```

Example output for no relevant results :

```
There is no relevant documents to your query
```

### SHOW

Downloads the content of the specified file.

#### Request

```
SHOW <filename>
```

#### Response

- `CONTENT <content>` : entire file content
- `FILE_DOESNT_EXIST` : file was not indexed
- `INVALID <message>` : malformed request

Example output for an existing file:

```
> SHOW file3.txt
Demanded document :
a bird is a beautiful animal that can fly
```

Example output for non-exist file

```
> SHOW file4.txt
Server does not has the demanded document
```

### UPLOAD

Uploads a new file to the server.

#### Request

```
UPLOAD <filename> <content>
```

#### Response

- `UPLOADED <filename>` : upload completed

### QUIT

Disconnect the server.

#### Request

```
QUIT
```

#### Response

The client immediately closes the connection.
No protocol message is sent by the server.

The client prints:

```
[Client] Closing connection and quitting...
```

## Example

## Docker Instructions

### Prerequisites

- Docker installed on your system
- Github account with a personal access token

<br>

---

### Build the Docker Image

```bash 
# Build the image locally with the tag "test"
docker build -t retrivium .  

# Test the build, and remove the image after xit
docker run --rm retrivium
```

<br>

--- 

### Create a Docker network

```bash
docker network create dai-retrivium
```

The output displays the network ID. Example output :

```
dfddf958a4be95c20546ab818bb0bc823a29d18e82e26d5f5bbe1b403851e1e9
```

### Publish to GitHub Container Registry

1. Create a personal access token on Github

    - Go to **Setting** -> **Developer settings**-> **Personal access tokens**
    - Select **Tokens(classic)** -> **Generate new token** -> **Generate new token (classic)**
    - Give the token a name (e.g., GitHub Container Registry) -> select the Expiration period -> select scopes :
        - **write:packages**
        - **read:packages**
        - **delete:packages**

    - Click **Generate token**

<br>

<img width="808" height="170" alt="docker_instuction_generate_token" src="https://github.com/user-attachments/assets/603e2584-70ea-46dc-b79d-89a4f0605277" />

<br>
<br>

 --- 

2. Login to GitHub Container Registry :

```bash
  # login GitHub Container Registry with your github user name
	docker login ghcr.io -u <your_github_username>
```

Then use the token you just created as password to login. Once login, you will see:

```bash 
	Login Succeeded
```

<br>

--- 

3. Tag the image for GitHub Container Registry

```bash
docker tag retrivium:latest ghcr.io/<username_in_lower_case>/retrivium:latest
```

<br>

--- 

4. List all the images (optional )

```bash
	docker images
```

Example output:

```
REPOSITORY                          TAG       IMAGE ID       CREATED        SIZE
ghcr.io/feliciacoding/retrivium     latest    c357c2fb3f4c   2 hours ago    426MB
```

<br>

--- 

5. Publish the image on GitHub Container Registry

```bash
docker push ghcr.io/<username>/retrivium:latest
```

Example output:

```bash
The push refers to repository [ghcr.io/feliciacoding/retrivium]
7f5571bd4564: Pushed 
f16370605504: Pushed 
91e052f7b40a: Pushed 
07df04fa1333: Pushed 
b5e329fb7a0e: Pushed 
97dd3f0ce510: Pushed 
d87284f77b3f: Pushed 
ee3225358e00: Pushed 
latest: digest: sha256:c357c2fb3f4c1aa91f18c066f94b3c6bf52818b001d1b779d90131da69b54965 size: 856
```

Now you can go to the GitHub Container Registry page of your repository to check that the image has been published
`https://github.com/<your_github_username>?tab=packages`

<img width="860" height="376" alt="docker_instruction_package_github" src="https://github.com/user-attachments/assets/e1c4745d-fe37-48b9-b07d-4ab057e91c6c" />

<br>

---

### Using the application with Docker

1. **Pull the Image**

```bash
docker pull ghcr.io/feliciacoding/retrivium:latest
```

<br>

--- 

2. **Build the image**

```bash
docker build -t retrivium .
```

3. **Verify the build**

```bash
docker run --rm retrivium
```

4. **Prepare data**
   Create a directory with text files that will be indexed and searchable.

```bash
mkdir -p data && echo "Machine learning content" > data/ml.txt
```

5. **Create network**
   Create a Docker network so the server and client containers can communicate.

```
docker network create dai-retrivium
```

6. **Start server**
   Run the server container that will index documents and listen for client connections.

```bash
# Use custom network, NO port publishing needed
docker run --rm -it --network dai-retrivium -v "$(pwd)/data:/app/data" --name my-server retrivium server --data-directory /app/data
```

7. **Start client (new terminal)**
   Connect a client to the server using the container name as hostname.

```bash 
# Run the client container
docker run --rm -it --network dai-retrivium retrivium client --host my-server --port 6433
```

8. **Test commands**
   Try searching and listing documents in the client prompt.

```bash 
# Type in the client terminal
> LIST
> QUIT
```

9. **Stop Server**
   Stop the server container when finished. To stop the server, press `Ctrl+C`, or run:

```bash
# Press Ctrl+C in server terminal or run
docker stop my-server
```

10. **Clean up**
    Remove the Docker network and data directory.

```bash
# Remove the network
docker network rm dai-retrivium
```

<br>

---

### Troubleshooting

- **Container name already in use:**
  If you see "name is already in use", stop the existing container:

```bash
docker stop retrivium-server
```

- **Port already in use:**
  If port 6433 is already in use, you can use a different port:

```bash 
docker run --rm -p 8080:6433 --name retrivium-server ghcr.io/feliciacoding/retrivium:latest server 
``` 

- **Check running containers:**

```bash 
docker ps 
```

## Advantage

- Multiple users
- Fast relevance ranking (BM25 algorithm)
- Real-time document uploads (UPLOAD command)
- Lightweight
- Text-based protocol (CLI)

## Use cases

- Code Snippet Repository sharing within Dev team
- Configuration File Finder sharing accross DevOp team
- Log File Analysis for specific error messages or events
- legal document search for law firms
- Multi-User Collaborative Search

## Authors

- [FeliciaCoding](https://github.com/FeliciaCoding)
- [maxmakovskiy](https://github.com/maxmakovskiy)
- [AlterSpectre](https://github.com/AlterSpectre)

kindly note the content of README file was generated with the help of ChatGPT.

---
