



## Docker Instructions 

### Prerequisites
- Docker installed on your system 
- Github account with a personal access token

<br>

---

### Build the Docker Image
```bash 
# Build the image locally with the tag "test"
docker build -t retrivium:test .  

# Test the build, and remove the image after xit
docker run --rm retrivium:test
```
<br>

--- 

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
docker tag retrivium:test ghcr.io/<username_in_lower_case>/retrivium:latest
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
  Now you can go to the GitHub Container Registry page of your repository to check that the image has been published`https://github.com/<your_github_username>?tab=packages`

<img width="860" height="376" alt="docker_instruction_package_github" src="https://github.com/user-attachments/assets/e1c4745d-fe37-48b9-b07d-4ab057e91c6c" />

<br>

---

### Using the application with Docker

1.  Pull the Image 
```bash
docker pull ghcr.io/feliciacoding/retrivium:latest
```
<br>

--- 

2. Run the server
```bash 
docker run --rm -p 6433:6433 --name retrivium-server ghcr.io/feliciacoding/retrivium:latest server
```

  The server is now listening on port **6433**

<br>

--- 


3. Run the Client In a separate terminal: 
```bash 
docker run --rm -it --link retrivium-server ghcr.io/feliciacoding/retrivium:latest client --host retrivium-server 
``` 

  You can now interact with the search engine through the REPL interface.

<br>
---

4. Using Data Files (if needed) 

```bash
# Server with data volume 
docker run --rm -p 6433:6433 -v "$(pwd)/data:/app/data" --name retrivium-server ghcr.io/feliciacoding/retrivium:latest server 

# Client connecting to server 
docker run --rm -it --link retrivium-server ghcr.io/feliciacoding/retrivium:latest client --host retrivium-server 
```
<br>

--- 

5. Stop the Application 
  To stop the server, press `Ctrl+C`, or run: 
```bash
docker stop retrivium-server
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
