import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import ts from 'typescript';
import { baseCompile } from '@intlify/message-compiler';

const localeRoot = 'src/i18n/locales';
const catalog = async (locale) => {
  const result = {};
  for (const file of fs.readdirSync(`${localeRoot}/${locale}`).filter(file => file.endsWith('.ts') && file !== 'index.ts')) {
    const source = fs.readFileSync(`${localeRoot}/${locale}/${file}`, 'utf8');
    const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true);
    const checkDuplicates = (node) => {
      if (ts.isObjectLiteralExpression(node)) {
        const names = node.properties.map(property => property.name?.getText(ast));
        assert.equal(new Set(names).size, names.length, `Duplicate message in ${locale}/${file}`);
      }
      ts.forEachChild(node, checkDuplicates);
    };
    checkDuplicates(ast);
    const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } }).outputText;
    const messages = (await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}`)).default;
    const flatten = (value, prefix) => {
      for (const [key, item] of Object.entries(value)) {
        const fullKey = `${prefix}.${key}`;
        if (typeof item === 'string') {
          baseCompile(item, { onError: error => { throw new Error(`${locale}/${fullKey}: ${error.message}`); } });
          result[fullKey] = item;
        } else flatten(item, fullKey);
      }
    };
    flatten(messages, path.basename(file, '.ts'));
  }
  return result;
};

const zh = await catalog('zh-CN');
const en = await catalog('en');
assert.deepEqual(Object.keys(en).sort(), Object.keys(zh).sort(), 'Locale message keys must match');
const placeholders = value => [...new Set([...value.matchAll(/\{(\w+)\}/g)].map(match => match[1]))].sort();
for (const key of Object.keys(zh)) assert.deepEqual(placeholders(en[key]), placeholders(zh[key]), `Placeholder mismatch: ${key}`);

let references = 0;
const checkSource = dir => {
  for (const file of fs.readdirSync(dir)) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      if (fullPath !== 'src/i18n') checkSource(fullPath);
    } else if (/\.(vue|ts)$/.test(file)) {
      const source = fs.readFileSync(fullPath, 'utf8');
      const patterns = [/(?:\bt|\btr|\btranslate)\(\s*['"]([\w.]+)['"]/g, /\bkeypath=['"]([\w.]+)['"]/g];
      for (const pattern of patterns) for (const match of source.matchAll(pattern)) {
        assert.ok(match[1] in zh, `Unknown message ${match[1]} in ${fullPath}`);
        references++;
      }
    }
  }
};
checkSource('src');
console.log(`Checked ${Object.keys(zh).length} messages in both languages and ${references} references.`);
