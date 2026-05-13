import tiktoken


class TextChunker:
    def __init__(self, chunk_size: int = 1000, chunk_overlap: int = 200) -> None:
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self._enc = tiktoken.get_encoding("cl100k_base")

    def chunk_file(self, file_path: str, content: str) -> list[dict]:
        if not content.strip():
            return []

        paragraphs = self._split_paragraphs(content)
        chunks: list[dict] = []
        current_tokens: list[int] = []
        current_start_char = 0
        current_text_parts: list[str] = []

        for paragraph in paragraphs:
            para_tokens = self._enc.encode(paragraph)

            if (
                current_tokens
                and len(current_tokens) + len(para_tokens) > self.chunk_size
            ):
                chunk_text = "".join(current_text_parts)
                chunks.append(
                    {
                        "content": chunk_text,
                        "chunk_index": len(chunks),
                        "metadata": {
                            "file_path": file_path,
                            "start_char": current_start_char,
                            "end_char": current_start_char + len(chunk_text),
                        },
                    }
                )

                overlap_tokens = current_tokens[-self.chunk_overlap :]
                overlap_text = self._enc.decode(overlap_tokens)
                current_start_char = (
                    current_start_char + len(chunk_text) - len(overlap_text)
                )
                current_tokens = overlap_tokens
                current_text_parts = [overlap_text]

            if len(para_tokens) > self.chunk_size:
                if current_tokens:
                    chunk_text = "".join(current_text_parts)
                    chunks.append(
                        {
                            "content": chunk_text,
                            "chunk_index": len(chunks),
                            "metadata": {
                                "file_path": file_path,
                                "start_char": current_start_char,
                                "end_char": current_start_char + len(chunk_text),
                            },
                        }
                    )
                    current_start_char += len(chunk_text)
                    current_tokens = []
                    current_text_parts = []

                self._chunk_long_paragraph(
                    para_tokens, file_path, current_start_char, chunks
                )
                current_start_char += len(paragraph)
                current_tokens = []
                current_text_parts = []
                continue

            current_tokens.extend(para_tokens)
            current_text_parts.append(paragraph)

        if current_tokens:
            chunk_text = "".join(current_text_parts)
            chunks.append(
                {
                    "content": chunk_text,
                    "chunk_index": len(chunks),
                    "metadata": {
                        "file_path": file_path,
                        "start_char": current_start_char,
                        "end_char": current_start_char + len(chunk_text),
                    },
                }
            )

        for i, chunk in enumerate(chunks):
            chunk["chunk_index"] = i

        return chunks

    def _split_paragraphs(self, content: str) -> list[str]:
        parts = content.split("\n\n")
        result: list[str] = []
        for i, part in enumerate(parts):
            if i < len(parts) - 1:
                result.append(part + "\n\n")
            else:
                result.append(part)
        return [p for p in result if p]

    def _chunk_long_paragraph(
        self,
        tokens: list[int],
        file_path: str,
        base_char_offset: int,
        chunks: list[dict],
    ) -> None:
        start = 0
        while start < len(tokens):
            end = min(start + self.chunk_size, len(tokens))
            chunk_text = self._enc.decode(tokens[start:end])
            char_offset = len(self._enc.decode(tokens[:start]))
            chunks.append(
                {
                    "content": chunk_text,
                    "chunk_index": len(chunks),
                    "metadata": {
                        "file_path": file_path,
                        "start_char": base_char_offset + char_offset,
                        "end_char": base_char_offset + char_offset + len(chunk_text),
                    },
                }
            )
            start += self.chunk_size - self.chunk_overlap
