"""Import only static BBI MEDIA contact and donation content without executing the source JSX."""
import ast
import json
from pathlib import Path
import re
import shutil
import sys


class LiteralReader:
    def __init__(self, text):
        self.text = text
        self.at = 0

    def skip(self):
        while self.at < len(self.text):
            if self.text[self.at].isspace():
                self.at += 1
            elif self.text.startswith('//', self.at):
                end = self.text.find('\n', self.at)
                self.at = len(self.text) if end < 0 else end + 1
            elif self.text.startswith('/*', self.at):
                end = self.text.find('*/', self.at + 2)
                if end < 0:
                    raise ValueError('Unclosed comment')
                self.at = end + 2
            else:
                return

    def take(self, char):
        self.skip()
        if self.text[self.at:self.at + 1] != char:
            raise ValueError(f'Expected {char} at {self.at}')
        self.at += 1

    def value(self):
        self.skip()
        char = self.text[self.at]
        if char in "\"'":
            begin = self.at
            self.at += 1
            while self.at < len(self.text):
                if self.text[self.at] == '\\':
                    self.at += 2
                elif self.text[self.at] == char:
                    self.at += 1
                    return ast.literal_eval(self.text[begin:self.at])
                else:
                    self.at += 1
            raise ValueError('Unclosed string')
        if char == '[':
            self.at += 1
            result = []
            while True:
                self.skip()
                if self.text[self.at] == ']':
                    self.at += 1
                    return result
                result.append(self.value())
                self.skip()
                if self.text[self.at] != ']':
                    self.take(',')
        if char == '{':
            self.at += 1
            result = {}
            while True:
                self.skip()
                if self.text[self.at] == '}':
                    self.at += 1
                    return result
                key = self.value()
                self.take(':')
                result[key] = self.value()
                self.skip()
                if self.text[self.at] != '}':
                    self.take(',')
        match = re.match(r'[A-Za-z_$][A-Za-z0-9_$]*|-?\d+(?:\.\d+)?', self.text[self.at:])
        if not match:
            raise ValueError(f'Only static literals are allowed at {self.at}')
        self.at += len(match[0])
        return {'true': True, 'false': False, 'null': None}.get(match[0], match[0])


def read_constant(text, name):
    match = re.search(r'\bconst\s+' + re.escape(name) + r'\s*=\s*', text)
    if not match:
        raise ValueError('Constant not found: ' + name)
    return LiteralReader(text[match.end():]).value()


def main():
    source = Path(sys.argv[1])
    target = Path(__file__).resolve().parents[1] / 'public' / 'assets'
    components = source / 'frontend' / 'src' / 'components'
    about_path = components / 'AboutView.jsx'
    about = about_path.read_text(encoding='utf-8-sig')
    data = {
        'company': read_constant(about, 'COMPANY'),
        'community': read_constant(about, 'COMMUNITY'),
        'donation': read_constant(about, 'DONATE'),
    }
    target.mkdir(exist_ok=True)
    for name in ('bbi-mark.png', 'bbi-logo.png', 'donate-qr.png'):
        shutil.copy2(source / 'frontend' / 'src' / 'assets' / name, target / name)
    (target / 'bbi-content.json').write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    print('Imported BBI MEDIA contact, community, donation details and three original images.')


if __name__ == '__main__':
    main()
