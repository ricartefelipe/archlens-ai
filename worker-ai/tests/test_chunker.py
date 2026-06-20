from app.chunking.chunker import TextChunker
from app.chunking.strategies import (
    ChunkerFactory,
    GenericChunker,
    JavaChunker,
    SqlChunker,
    TerraformChunker,
    YamlChunker,
)


def test_empty_content_yields_no_chunks():
    assert TextChunker().chunk_file("a.txt", "   \n\n  ") == []


def test_short_content_yields_single_chunk_with_metadata():
    chunks = TextChunker().chunk_file("notes.txt", "linha unica de conteudo")

    assert len(chunks) == 1
    assert chunks[0]["chunk_index"] == 0
    assert chunks[0]["content"] == "linha unica de conteudo"
    assert chunks[0]["metadata"]["file_path"] == "notes.txt"


def test_chunk_indexes_are_sequential():
    text = "\n\n".join(f"parágrafo número {i} com algum texto" for i in range(50))
    chunker = TextChunker(chunk_size=20, chunk_overlap=5)

    chunks = chunker.chunk_file("doc.txt", text)

    assert len(chunks) > 1
    assert [c["chunk_index"] for c in chunks] == list(range(len(chunks)))


def test_chunker_factory_selects_strategy_by_extension():
    assert isinstance(ChunkerFactory.get_chunker("Service.java"), JavaChunker)
    assert isinstance(ChunkerFactory.get_chunker("config.yml"), YamlChunker)
    assert isinstance(ChunkerFactory.get_chunker("001.sql"), SqlChunker)
    assert isinstance(ChunkerFactory.get_chunker("main.tf"), TerraformChunker)
    assert isinstance(ChunkerFactory.get_chunker("prod.tfvars"), TerraformChunker)
    assert isinstance(ChunkerFactory.get_chunker("README.md"), GenericChunker)


def test_java_chunker_splits_on_declarations():
    content = (
        "public class A {\n"
        "    public void foo() { return; }\n"
        "    public void bar() { return; }\n"
        "}\n"
    )
    chunks = JavaChunker().chunk_file("A.java", content)

    assert len(chunks) >= 2
    assert [c["chunk_index"] for c in chunks] == list(range(len(chunks)))
