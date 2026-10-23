from pydantic import BaseModel


class UploadSummary(BaseModel):
    status: str
    filename: str
    size: str
    lines: int
