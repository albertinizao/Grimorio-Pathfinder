from __future__ import annotations

import os
import shutil
import subprocess
import sys
import socket
import tempfile
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path

from playwright.sync_api import TimeoutError as PlaywrightTimeoutError
from playwright.sync_api import sync_playwright


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_DIR = ROOT / "frontend"
TEST_FIXTURE_GENERATED = ROOT / "src" / "test" / "resources" / "data" / "generated" / "spells-es.generated.json"
TEST_FIXTURE_OVERRIDES = ROOT / "src" / "test" / "resources" / "data" / "overrides" / "spells-es.overrides.json"


@dataclass
class ManagedProcess:
    name: str
    process: subprocess.Popen[str]
    log_path: Path
    log_file: object


def main() -> int:
    backend_port = find_free_port()
    frontend_port = find_free_port(exclude={backend_port})
    temp_dir = Path(tempfile.mkdtemp(prefix="grimorio-smoke-"))
    try:
        generated_path = temp_dir / "spells-es.generated.json"
        overrides_path = temp_dir / "spells-es.overrides.json"
        sqlite_path = temp_dir / "grimorio.sqlite"
        shutil.copy2(TEST_FIXTURE_GENERATED, generated_path)
        shutil.copy2(TEST_FIXTURE_OVERRIDES, overrides_path)

        backend_log = temp_dir / "backend.log"
        frontend_log = temp_dir / "frontend.log"
        backend = start_process(
            name="backend",
            command=["cmd", "/c", ".\\mvnw.cmd", "spring-boot:run"],
            cwd=ROOT,
            env={
                "SERVER_PORT": str(backend_port),
                "GRIMORIO_DATASET_GENERATED_PATH": str(generated_path),
                "GRIMORIO_DATASET_OVERRIDES_PATH": str(overrides_path),
                "GRIMORIO_SQLITE_PATH": str(sqlite_path),
                "GRIMORIO_SQLITE_AUTO_REBUILD": "true",
            },
            log_path=backend_log,
        )
        frontend = start_process(
            name="frontend",
            command=["cmd", "/c", "npm", "run", "dev", "--", "--host", "127.0.0.1", "--port", str(frontend_port)],
            cwd=FRONTEND_DIR,
            env={
                "VITE_API_BASE_URL": f"http://127.0.0.1:{backend_port}",
            },
            log_path=frontend_log,
        )

        try:
            wait_for_http(f"http://127.0.0.1:{backend_port}/api/spell-lists?listType=CLASS", "Clérigo")
            wait_for_http(f"http://127.0.0.1:{frontend_port}")
            return run_browser_flow(frontend_port)
        except Exception:
            print(f"Backend log: {backend.log_path}", file=sys.stderr)
            print(f"Frontend log: {frontend.log_path}", file=sys.stderr)
            raise
        finally:
            terminate_process(frontend)
            terminate_process(backend)
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)


def run_browser_flow(frontend_port: int) -> int:
    base_url = f"http://127.0.0.1:{frontend_port}"
    note_text = "Smoke test note drow 2026-06-11"
    updated_description = "El objetivo queda temporalmente protegido contra el veneno. [E2E smoke]"

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1400, "height": 1200})
        try:
            page.goto(base_url, wait_until="networkidle")
            page.get_by_role("heading", name="Edición local de conjuros").wait_for(timeout=30_000)

            page.wait_for_function(
                """() => document.querySelectorAll('.search-form select option').length > 0""",
                timeout=30_000,
            )

            search_form = page.locator(".search-form")
            search_form.locator("select").nth(0).select_option(value="Clérigo")
            search_form.locator("select").nth(1).select_option(value="3")
            search_form.locator('input[type="text"]').fill("drow")
            page.get_by_role("button", name="Buscar").click()

            result_card = page.locator(".result-card").filter(has_text="Retrasar veneno")
            result_card.wait_for(timeout=30_000)
            result_card.click()

            page.get_by_role("heading", name="Retrasar veneno").wait_for(timeout=30_000)
            page.get_by_label("Descripción española").fill(updated_description)

            page.get_by_role("button", name="Guardar campos").click()
            wait_for_banner(page, "Campos españoles guardados.")

            page.get_by_label("personalNotes").fill(note_text)
            page.get_by_role("button", name="Guardar notas").click()
            wait_for_banner(page, "Notas personales guardadas.")
            assert page.get_by_label("personalNotes").input_value() == note_text

            page.get_by_label("translationStatus").select_option(value="LOCKED")
            page.get_by_role("button", name="Guardar estado").click()
            wait_for_banner(page, "Estado actualizado.")

            page.locator(".detail-panel .muted").filter(has_text="Bloqueado").wait_for(timeout=30_000)
            assert page.get_by_label("Descripción española").input_value() == updated_description
            assert page.get_by_label("translationStatus").input_value() == "LOCKED"

            page.get_by_role("button", name="Volver a resultados").click()

            assert page.locator(".search-form").locator('input[type="text"]').input_value() == "drow"
            assert page.locator(".search-form").locator("select").nth(0).input_value() == "Clérigo"
            assert page.locator(".search-form").locator("select").nth(1).input_value() == "3"

            updated_result = page.locator(".result-card").filter(has_text="Retrasar veneno")
            updated_result.wait_for(timeout=30_000)
            assert "LOCKED" in updated_result.inner_text()
            assert note_text in updated_result.inner_text()

            print("Smoke E2E passed: spell-editing-ui flow is working.")
            return 0
        finally:
            browser.close()


def wait_for_banner(page, expected_text: str, timeout_ms: int = 30_000) -> None:
    banner = page.get_by_role("status")
    banner.filter(has_text=expected_text).wait_for(timeout=timeout_ms)


def start_process(name: str, command: list[str], cwd: Path, env: dict[str, str], log_path: Path) -> ManagedProcess:
    log_file = open(log_path, "w", encoding="utf-8")
    process_env = os.environ.copy()
    process_env.update(env)
    process_env["CI"] = "true"
    process = subprocess.Popen(
        command,
        cwd=str(cwd),
        env=process_env,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        text=True,
    )
    return ManagedProcess(name=name, process=process, log_path=log_path, log_file=log_file)


def terminate_process(managed: ManagedProcess) -> None:
    if managed.process.poll() is None:
        if os.name == "nt":
            subprocess.run(
                ["taskkill", "/PID", str(managed.process.pid), "/T", "/F"],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
        else:
            managed.process.terminate()
            try:
                managed.process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                managed.process.kill()
                managed.process.wait(timeout=30)
    try:
        managed.log_file.close()
    except Exception:
        pass


def find_free_port(exclude: set[int] | None = None) -> int:
    excluded = exclude or set()
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    if port in excluded:
        return find_free_port(exclude=excluded)
    return port


def wait_for_http(url: str, expected_fragment: str | None = None, timeout_seconds: int = 180) -> None:
    deadline = time.time() + timeout_seconds
    last_error: Exception | None = None
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=5) as response:
                body = response.read().decode("utf-8", errors="ignore")
                if expected_fragment is None or expected_fragment in body:
                    return
        except (urllib.error.URLError, TimeoutError, ConnectionRefusedError, OSError) as exc:
            last_error = exc
        time.sleep(2)
    raise RuntimeError(f"Timed out waiting for {url}") from last_error


if __name__ == "__main__":
    raise SystemExit(main())
