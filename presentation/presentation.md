---
theme: default
_class: lead
paginate: true
backgroundColor: #fff
---

# BM25 Search Engine

## Fast and lightweight BM25-powered document search over TCP

## Authors: Liao Pei-Wen, Makovskyi Maksym, Wu Guo Yu

---

# What problem we are trying to solve ?

<style scoped>
p { text-align: center; }
</style>

<br/>

![fileSearingImage_pexels.jpg](img/fileSearingImage_pexels.jpg)

---

# Our solution to the problem

<style scoped>
p { text-align: center; }
</style>

<br/>


A **TCP-based search engine** powered by **BM25 relevance ranking**, enabling:

![Solution.png](img/Solution.png)

- Real-time document indexing
- Multi-user access via TCP
- Relevance-based search (`QUERY`)
- Document browsing (`SHOW`)
- Dynamic file uploads (`UPLOAD`)
---


# How to use ?

### Using the application with Docker

1. **Pull the Image**

```bash
docker pull ghcr.io/feliciacoding/retrivium:latest
```

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


---

# How it works ? (build 1)

![retrivium_app.png](img/retrivium_app.png)

1. Read the content of files

```
"a cat is a feline and likes to eat bird",            // file1.txt
"a dog is the human's best friend and likes to play", // file2.txt
"a bird is a beautiful animal that can fly",          // file3.txt
```

---

# How it works ? (build 2)

2. split them
3. avoid meaningless words (is/a/to/etc)
4. stem them (connections, connected, connecting -> connect)

```
[
    ["cat", "felin", "like", "eat", "bird"],            // file1.txt
    ["dog", "human", "best", "friend", "like", "plai"], // file2.txt
    ["bird", "beauti", anim", "can", "fly"]             // file3.txt
]
```

---

# How it works ? (build 3)

5. build vocabulary

```
corpus = [
    ["cat", "felin", "like", "eat", "bird"],
    ["dog", "human", "best", "friend", "like", "plai"],
    ["bird", "beauti", anim", "can", "fly"]
]

vocabulary = [
    "like", "best", "plai", "can", "fly", "beauti", 
    "cat", "bird", "friend", "eat", "anim", "dog", "human", "felin"
]
```

---

# How it works ? (build 4)

6. For every token in every document compute BM25 scores
   <br>

$$
log(\frac{N - df_t + 0.5}{df_t + 0.5} + 1) \cdot \frac{tf_{td}}{ k_1 \cdot (1 - b + b \cdot ( \frac{ L_d }{ L_{avg} } )) + tf_{td} }
$$

---

# How it works ? (build 5)

6. Build document-term matrix with resulting BM25 scores

| docIdx | like | best | plai | can  | fly  | beauti | cat  | bird |
|--------|------|------|------|------|------|--------|------|------|
| 0      | 0.22 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00   | 0.46 | 0.22 |
| 1      | 0.20 | 0.42 | 0.42 | 0.00 | 0.00 | 0.00   | 0.00 | 0.00 |
| 2      | 0.00 | 0.00 | 0.00 | 0.46 | 0.46 | 0.46   | 0.00 | 0.22 |


| docIdx | friend | eat  | anim | dog  | human | felin |
|--------|--------|------|------|------|-------|-------|
| 0      | 0.00   | 0.46 | 0.00 | 0.00 | 0.00  | 0.46  |
| 1      | 0.42   | 0.00 | 0.00 | 0.42 | 0.42  | 0.00  |
| 2      | 0.00   | 0.00 | 0.46 | 0.00 | 0.00  | 0.00  |

---

# How it works ? (search 1)

1. Tokenize query

```
// From :
"Which animal is the human best friend?"

// To
[ "anim", "human", "best", "friend" ]
```

---

# How it works ? (search 2)

2. Iterate over document-term matrix and accumulate corresponding tokens.

| docIdx | best | friend | anim | human | Result |
|--------|------|--------|------|-------|--------|
| 0      | 0.00 | 0.00   | 0.00 | 0.00  | 0.00   |
| 1      | 0.42 | 0.42   | 0.00 | 0.42  | 1.26   |
| 2      | 0.00 | 0.00   | 0.46 | 0.00  | 0.46   |




---

# Use Cases

- Personal Document Library
- Email Archive Search
- Company Knowledge Base
- Real Estate Listings
- Customer Support Ticket System

---


# Roadmap - Current Limitations & Planned Improvements (1)

- **Unit testing**


- **Include files in subfolders**

[Limitation] Doesn't read subfolders

```
build -I index.txt documents --recursive
```

---

# Roadmap - Current Limitations & Planned Improvements (2)

- **Custom stopwords + turn off stemming**

[Limitation] stopwords List is hard-coded

```
    build -I index.txt documents --stopwords my_words.txt --stem-off
```
- **Friendlier search results**

[Limitation] Doesn't Show what’s inside the index.

```
file2.txt score=1.26 [matched: dog, human, friend] "...dog is the human's best friend..."
```

---

# Roadmap - Current Limitations & Planned Improvements (3)

- **Compressed sparse column (CSC) storage**

  [Limitation] plain-text LIL with string tokens


---

<br>

# Thank you for your attention !


