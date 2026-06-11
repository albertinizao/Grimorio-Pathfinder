from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_DIR = ROOT / "frontend"


def run_step(label: str, command: list[str], cwd: Path) -> None:
    print(f"==> {label}")
    subprocess.run(command, cwd=str(cwd), check=True)


def main() -> int:
    is_windows = os.name == "nt"
    mvnw = [".\\mvnw.cmd"] if is_windows else ["./mvnw"]
    npm = ["npm.cmd"] if is_windows else ["npm"]

    try:
        run_step("Backend tests", mvnw + ["test"], ROOT)
        run_step("Frontend typecheck", npm + ["run", "typecheck"], FRONTEND_DIR)
        run_step("Frontend build", npm + ["run", "build"], FRONTEND_DIR)
        run_step("Smoke E2E spell-editing-ui", [sys.executable, str(ROOT / "scripts" / "smoke_spell_editing.py")], ROOT)
    except subprocess.CalledProcessError as error:
        return error.returncode

    print("Verificación local completada.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
