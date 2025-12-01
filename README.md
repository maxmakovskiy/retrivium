<div align="center">
  <img src="bm25.png" alt="Best Matching 25-th iteration" width="400">

# BM25 Search library

**A fast and lightweight BM25-based library to search through the corpus of provided files implemented in JAVA**
</div>


---

## Description

From [Wikipedia](https://en.wikipedia.org/wiki/Okapi_BM25):
> **BM25** is a bag-of-words retrieval function that ranks a set of documents based on the query terms appearing in each document, regardless of their proximity within the document.


---

## Table of Contents

1. [Description](#description)
2. [Requirements](#requirements)
3. [Integration](#integration)
4. [Example](#example)
5. [Formula](#formula)
6. [How it works](#how-it-works)
7. [Repo structure](#repository-structure)
8. [Roadmap](#roadmap)
9. [How to contribute](#how-to-contribute)
10. [Dependencies](#dependencies)
11. [Authors](#authors)
12. [Acknowledgements](#acknowledgements)


---


## Requirements

Minimum Java version is Java 21.

---


## Integration

This project is being via GitHub's packages.
As it is clearly stated in [this discussion](https://github.com/orgs/community/discussions/26634#discussioncomment-8527086) GitHub does not support unauthoticated access to the GitHub's packages, even to the public ones. So, you need to setup personal access token with `read:packages` permission to be able to read from the GitHub's packages. How to accomplish it is explained [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token).

Add next lines to your `pom.xml` to define where this library comes from:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub maxmakovskiy Maven Packages</name>
        <url>https://maven.pkg.github.com/maxmakovskiy/bm25</url>
    </repository>
</repositories>
```

as well as the lines to declare dependency itself:

```xml
<dependency>
    <groupId>ch.heigvd.dai</groupId>
    <artifactId>bm25</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

More information on maven `repositories` setting could be found [here](https://maven.apache.org/guides/introduction/introduction-to-repositories.html).

---


## Example

#### Building index

```java
ArrayList<String> docNames = new ArrayList<>(List.of(
    "file1.txt", "file2.txt", "file3.txt"
));

ArrayList<String> docs = new ArrayList<>(List.of(
    "a cat is a feline and likes to eat bird",
    "a dog is the human's best friend and likes to play",
    "a bird is a beautiful animal that can fly"
));

BM25 bm25 = new BM25();
ArrayList<ArrayList<String>> corpusTokens = bm25.tokenize(docs);
bm25.buildIndex(corpusTokens, docNames);

```

#### Using index

```java
String query = "Which animal is the human best friend?";

ArrayList<RankingResult> results = bm25.retrieveTopK(
    bm25.tokenize(query), topK);

for (RankingResult result : results) {
    int docIdx = result.getDocIndex();
    double score = result.getScore();
    System.out.println(
        "file : "
            + bm25.getIndex().getDocumentName(docIdx)
            + " => score = " + score);
}

```

#### Convert index to JSON and back

```java
// forth
String indexStr = bm25.getIndex().toJSON();

// back
Index index = Index.fromJSON(indexBuilder.toString());
// so latter we could restore BM25 with this index
BM25 bm25 = new BM25(index);

```

---


## How it works?

The task of ranking documents/books/notes/etc with the respect to certain query is very natural for humans, we do it all the time.

### Phase 1: Building the Index

#### 1. Input Documents

Supposing that user has a collection of documents, and he wants to search for some information.      
For example (not my example, [see](https://stackoverflow.com/a/78680638)), let's say each document has just one line:

````
    "a cat is a feline and likes to eat bird",            // file1.txt
    "a dog is the human's best friend and likes to play", // file2.txt
    "a bird is a beautiful animal that can fly"           // file3.txt
````

#### 2. Tokenization

Documents are processed through:
- **Splitting**: Text is split into individual words
- **Stop word removal**: Common words like "a", "is", "the" are removed
- **Stemming**: Words are reduced to their root form (e.g., "likes" → "like", "beautiful" → "beauti")

**Result (corpus):**
````
[
    ["cat", "feline", "like", "eat", "bird"],           // file1.txt
    ["dog", "human", "best", "friend", "like", "plai"], // file2.txt
    ["bird", "beauti", anim", "can", "fly"]             // file3.txt
]
````

#### 3. Vocabulary Construction

A vocabulary is built from all unique tokens:
````
like best plai can fly beauti cat bird friend eat anim dog human felin
````

#### 4. BM25 Score Matrix Construction

A **document-term matrix** is created where:
- Each row represents a document
- Each column represents a token from the vocabulary
- Each cell contains the BM25 score for that term in that document (0 if term is absent)  
  It is some sort of [document-term matrix](https://en.wikipedia.org/wiki/Document-term_matrix), but instead of the frequency of terms we store BM25 score.

**Score Matrix:**
| docIdx | like | best | plai | can  | fly  | beauti | cat  | bird | friend | eat  | anim | dog  | human | felin |
|--------|------|------|------|------|------|--------|------|------|--------|------|------|------|-------|-------|
| 0      | 0.22 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00   | 0.46 | 0.22 | 0.00   | 0.46 | 0.00 | 0.00 | 0.00  | 0.46  |
| 1      | 0.20 | 0.42 | 0.42 | 0.00 | 0.00 | 0.00   | 0.00 | 0.00 | 0.42   | 0.00 | 0.00 | 0.42 | 0.42  | 0.00  |
| 2      | 0.00 | 0.00 | 0.00 | 0.46 | 0.46 | 0.46   | 0.00 | 0.22 | 0.00   | 0.00 | 0.46 | 0.00 | 0.00  | 0.00  |

---

### Phase 2: Searching

#### 1. Query Processing

```
Query : "Which animal is the human best friend?"
```
After tokenization and stop word removal: `["anim", "human", "best", "friend"]`

#### 2. Score Calculation

For each document:
- Look up BM25 scores for query tokens
- Sum the scores for all query tokens present in the document

**Example calculation:**
```
file : file1.txt => score = 0.00
file : file2.txt => score = 1.26
file : file3.txt => score = 0.46
```

#### 3. Ranking

Documents are sorted by score in descending order:
```
Rank 1: file2.txt => score = 1.26  -> Most relevant
Rank 2: file3.txt => score = 0.46
Rank 3: file1.txt => score = 0.00
```

---


## Repository Structure

````
java/                                      // source
│   ├── ch.heigvd.dai.bm25/
│   │   ├── utils/                         // different utils used along the way
│   │   │   ├── Index.java                 // index abstraction
│   │   │   ├── DSparseMatrixLIL.java      // sparse matrix with LIL storage
│   │   │   ├── RankingResult.java         // (document index, score) pair in ranking results
│   │   │   ├── Stopword.java              // inessential words
│   │   ├── BM25.java                      // BM25 algorithm
│
test/                                      // unit-tests
│   ├── ch.heigvd.dai.bm25/
│   │   ├── utils/
│   │   │   ├── IndexTest.java             // test cases for index
│   │   │   ├── DSparseMatrixLILTest.java  // test cases for sparse matrix
│   │   ├── BM25Test.java                  // test cases for BM25 algorithm
````
---

## Roadmap

1. Custom stopwords
2. Option to either enable or disable stemming.
3. Performance — treat tokens as IDs (ints) and switch storage from LInked List to the CSC.
4. All the variants of BM25

---

## How to contribute

Please read corresponding [wiki page](#)

---


## Dependencies

- [Apache OpenNLP Tools v2.5.5](https://opennlp.apache.org/) for removing morphological affixes from words, leaving only the word stem
- [Jakson Databind v2.20.0](https://github.com/FasterXML/jackson-databind) for working with JSON.

---


## Authors

- [FeliciaCoding](https://github.com/FeliciaCoding)
- [maxmakovskiy](https://github.com/maxmakovskiy)
- [AlterSpectre](https://github.com/AlterSpectre)

kindly note the project graphic / charts are generated with the help of ChatGPT.

---


## Acknowledgements

This project is inspired by and heavily relies on the ideas presented in [bm25s](https://github.com/xhluca/bm25s).     
Given that it would be fair to said that it is some kind of adaption of the project mentioned above in Java.
Although a lot of things have not been respected for the sake of simplicity.


