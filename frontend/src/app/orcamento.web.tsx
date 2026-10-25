import { useEffect, useRef, useState } from 'react';
import Head from 'expo-router/head';
import { useLocalSearchParams } from 'expo-router';
import {
  consultarOrcamento, decidirOrcamento, ErroOrcamento, LINK_INDISPONIVEL,
  podeDecidir, validarMotivo, nomeUnidade, type OrcamentoPublico,
  consultarEmpresaPublica, getLogoEmpresaPublicaUrl, baixarArquivoPublico, type EmpresaPublica,
} from '../lib/orcamentoPublicoService';

const moeda = (valor: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
const data = (valor: string) => new Date(valor).toLocaleString('pt-BR');
const rotuloStatus = (status: string) => ({
  AGUARDANDO_APROVACAO: 'Aguardando sua decisão', APROVADO: 'Proposta já aprovada',
  REJEITADO: 'Proposta já recusada',
}[status] ?? 'Proposta indisponível para alteração');

export default function OrcamentoRoute() {
  const { token } = useLocalSearchParams<{ token?: string | string[] }>();
  const valor = typeof token === 'string' ? token.trim() : '';
  return <PaginaOrcamento key={valor} token={valor} />;
}
function PaginaOrcamento({ token }: { token: string }) {
  const [orcamento, setOrcamento] = useState<OrcamentoPublico | null>(null);
  const [carregando, setCarregando] = useState(Boolean(token));
  const [erro, setErro] = useState(token ? '' : LINK_INDISPONIVEL);
  const [indisponivel, setIndisponivel] = useState(!token);
  const [tentativa, setTentativa] = useState(0);
  const [acao, setAcao] = useState<'aprovar' | 'rejeitar' | null>(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [sucesso, setSucesso] = useState('');
  const [dataDecisao, setDataDecisao] = useState('');
  const [empresa, setEmpresa] = useState<EmpresaPublica | null>(null);
  const [erroEmpresa, setErroEmpresa] = useState('');
  const [baixandoArquivoId, setBaixandoArquivoId] = useState<string | null>(null);
  const bloqueio = useRef(false);
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    let ativo = true;
    if (!token) return;
    consultarEmpresaPublica(token).then(resultado => {
      if (ativo) {
        setEmpresa(resultado);
        setErroEmpresa('');
      }
    }).catch((error: unknown) => {
      if (ativo) {
        setErroEmpresa(error instanceof Error ? error.message : 'Não foi possível carregar os dados da empresa.');
      }
    });
    return () => { ativo = false; };
  }, [token]);

  useEffect(() => {
    let ativo = true;
    if (!token) return;
    consultarOrcamento(token).then(resultado => {
      if (ativo) setOrcamento(resultado);
    }).catch((error: unknown) => {
      if (!ativo) return;
      setErro(error instanceof Error ? error.message : 'Não foi possível carregar a proposta.');
      setIndisponivel(error instanceof ErroOrcamento && error.indisponivel);
    }).finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [token, tentativa]);

  useEffect(() => {
    if (acao) dialog.current?.showModal();
    else dialog.current?.close();
  }, [acao]);

  async function confirmar() {
    if (!acao || !orcamento || !podeDecidir(orcamento.status) || bloqueio.current) return;
    if (acao === 'rejeitar') {
      const mensagem = validarMotivo(motivo);
      if (mensagem) { setErro(mensagem); return; }
    }
    bloqueio.current = true; setEnviando(true); setErro('');
    try {
      const resultado = await decidirOrcamento(token, acao, motivo);
      setOrcamento({ ...orcamento, status: resultado.status });
      setDataDecisao(resultado.data_decisao);
      setSucesso(resultado.status === 'APROVADO' ? 'Orçamento aprovado com sucesso!' : 'Orçamento rejeitado com sucesso!');
      setAcao(null);
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível enviar sua decisão.');
      if (error instanceof ErroOrcamento && error.indisponivel) {
        setIndisponivel(true); setOrcamento(null); setAcao(null);
      }
    } finally { bloqueio.current = false; setEnviando(false); }
  }
  return (
    <main className="orcamento-publico">
      <Head><title>Seu orçamento | Stentio</title><meta name="referrer" content="no-referrer" /><meta name="robots" content="noindex, nofollow" /></Head>
      <style>{css}</style>
      <div className="op-container">
        <header className="op-brand">
          <div className="op-brand-left">
            {empresa?.logo_disponivel && token ? (
              <img src={getLogoEmpresaPublicaUrl(token)} alt={empresa.nome || 'Logo da empresa'} className="op-logo" />
            ) : null}
            <div>{empresa?.nome || 'STENTIO'} <span>PROPOSTA COMERCIAL</span></div>
          </div>
          {(empresa?.endereco || empresa?.telefone) ? (
            <div className="op-brand-info">
              {empresa.endereco ? <div>{empresa.endereco}</div> : null}
              {empresa.telefone ? <div>{empresa.telefone}</div> : null}
            </div>
          ) : <span>PROPOSTA COMERCIAL</span>}
        </header>
        {erroEmpresa ? <p className="op-error op-company-error" role="alert">{erroEmpresa}</p> : null}
        <section className="op-card" aria-busy={carregando}>
          {carregando ? <p role="status">Carregando seu orçamento…</p> : indisponivel ? (
            <><h1>Link indisponível</h1><p role="alert">{erro}</p></>
          ) : !orcamento ? (
            <><h1>Não foi possível carregar</h1><p role="alert">{erro}</p>
              <button onClick={() => { setCarregando(true); setErro(''); setTentativa(n => n + 1); }}>Tentar novamente</button></>
          ) : (
            <>
              <div className="op-heading"><div><p className="op-eyebrow">PREPARADO PARA VOCÊ</p>
                <h1>Seu orçamento</h1><p>Confira os serviços e registre sua decisão abaixo.</p></div>
                <span className="op-status">{rotuloStatus(orcamento.status)}</span></div>
              <p className="op-id">Referência: {orcamento.id}{orcamento.numero_versao ? <> · Versão {orcamento.numero_versao}</> : null}{orcamento.criado_em ? <> · Emitido em {data(orcamento.criado_em)}</> : null}</p>
              {sucesso && <p className="op-success" role="status">{sucesso}{dataDecisao && <><br /><small>Decisão registrada em {data(dataDecisao)}.</small></>}</p>}
              {erro && !acao && <p className="op-error" role="alert">{erro}</p>}
              <div className="op-solicitacao">
                  <h2>Solicitação</h2>
                  {orcamento.solicitacao ? <>
                    <div className="op-solicitacao-grid">
                    {orcamento.solicitacao.categoria?.nome ? <div><span>Categoria</span><strong>{orcamento.solicitacao.categoria.nome}</strong></div> : null}
                    {orcamento.solicitacao.data_recebimento ? <div><span>Recebido em</span><strong>{data(orcamento.solicitacao.data_recebimento)}</strong></div> : null}
                    {(orcamento.solicitacao.idioma_origem?.nome && orcamento.solicitacao.idioma_destino?.nome) ? <div className="op-idiomas"><span>Idiomas</span><strong>{orcamento.solicitacao.idioma_origem.nome} ({orcamento.solicitacao.idioma_origem.codigo_iso?.toUpperCase()}) → {orcamento.solicitacao.idioma_destino.nome} ({orcamento.solicitacao.idioma_destino.codigo_iso?.toUpperCase()})</strong></div> : null}
                    </div>
                    {orcamento.solicitacao.descricao ? <div className="op-descricao"><span>Descrição / Instruções</span><p>{orcamento.solicitacao.descricao}</p></div> : null}
                    {orcamento.solicitacao.arquivos && orcamento.solicitacao.arquivos.length > 0 ? (
                      <div className="op-arquivos">
                        <span>Anexos ({orcamento.solicitacao.arquivos.length})</span>
                        <ul>
                          {orcamento.solicitacao.arquivos.map(arq => (
                            <li key={arq.id}>
                              <button type="button" disabled={baixandoArquivoId === arq.id} onClick={async () => {
                                try {
                                  setBaixandoArquivoId(arq.id);
                                  await baixarArquivoPublico(token, arq.id, arq.nome);
                                } catch (e) {
                                  console.error(e);
                                } finally {
                                  setBaixandoArquivoId(null);
                                }
                              }}>
                                {baixandoArquivoId === arq.id ? 'Baixando...' : arq.nome}
                              </button>
                              <small>{arq.tamanho_bytes ? `${Math.round(arq.tamanho_bytes/1024)} KB` : ''}</small>
                            </li>
                          ))}
                        </ul>
                      </div>
                    ) : null}
                  </> : <p className="op-muted">Esta proposta não possui uma solicitação vinculada.</p>}
                </div>
              <div className="op-table" tabIndex={0} role="region" aria-label="Itens do orçamento">
                <table>
                  <caption>Serviços de tradução</caption>
                  <thead><tr><th scope="col">Serviço</th><th scope="col">Quantidade</th><th scope="col">Unidade</th><th scope="col">Valor unitário</th><th scope="col">Subtotal</th></tr></thead>
                  <tbody>{orcamento.itens.map((item, index) => (
                    <tr key={index}><td>{item['tipo_de_serviço']}</td><td>{new Intl.NumberFormat('pt-BR').format(item.quantidade)}</td>
                      <td>{nomeUnidade(item.unidade)}</td><td>{moeda(item.valor_unitario)}</td><td>{moeda(item.subtotal)}</td></tr>
                  ))}{orcamento.itens.length === 0 && <tr><td colSpan={5}>Nenhum item informado.</td></tr>}</tbody>
                </table>
              </div>
              <div className="op-summary"><div><p><strong>Validade do link</strong><br />{data(orcamento.data_expiracao)}</p></div>
                <div className="op-total"><span>Valor total</span><strong>{moeda(orcamento.valor_total)}</strong></div></div>
              {podeDecidir(orcamento.status) ? <div className="op-actions">
                <p>Você poderá revisar a decisão antes de confirmar.</p>
                <div><button className="op-secondary" onClick={() => { setErro(''); setMotivo(''); setAcao('rejeitar'); }}>Rejeitar Orçamento</button>
                  <button onClick={() => { setErro(''); setAcao('aprovar'); }}>Aprovar Orçamento</button></div>
              </div> : <p className="op-closed">A decisão desta proposta não pode mais ser alterada por este link.</p>}
            </>
          )}
        </section>
        <footer>STENTIO · Gestão de serviços de tradução</footer>
      </div>
      <dialog ref={dialog} aria-labelledby="op-dialog-title" onCancel={event => { event.preventDefault(); if (!enviando) setAcao(null); }}>
        <h2 id="op-dialog-title">{acao === 'rejeitar' ? 'Rejeitar orçamento?' : 'Aprovar orçamento?'}</h2>
        <p>Confirme sua decisão. Após o envio, não será possível alterá-la por este link.</p>
        {acao === 'rejeitar' && <><label htmlFor="op-motivo">Motivo da rejeição (obrigatório)</label>
          <textarea id="op-motivo" autoFocus value={motivo} maxLength={2000} disabled={enviando} rows={5}
            onChange={event => setMotivo(event.target.value)} aria-describedby="op-limite" />
          <small id="op-limite">{motivo.length}/2000 caracteres</small></>}
        {erro && acao && <p className="op-error" role="alert">{erro}</p>}
        <div className="op-dialog-actions"><button className="op-secondary" disabled={enviando} onClick={() => { setAcao(null); setErro(''); }}>Cancelar</button>
          <button disabled={enviando} onClick={confirmar}>{enviando ? 'Enviando…' : acao === 'rejeitar' ? 'Confirmar rejeição' : 'Confirmar aprovação'}</button></div>
      </dialog>
    </main>
  );
}

const css = `
.orcamento-publico{min-height:100vh;background:#f6f3ef;color:#292421;font:16px/1.6 system-ui,sans-serif;padding:32px 20px;box-sizing:border-box;overflow:auto}
.orcamento-publico *{box-sizing:border-box}
.orcamento-publico .op-container{max-width:1060px;margin:auto}
.orcamento-publico .op-brand{display:flex;justify-content:space-between;gap:16px;color:#8c5230;font-weight:800;font-size:24px;letter-spacing:3px;margin-bottom:28px}
.orcamento-publico .op-brand span{font-size:11px;letter-spacing:2px;align-self:center}
.orcamento-publico .op-card{background:#fff;border:1px solid #e9e1d8;border-radius:20px;padding:40px;box-shadow:0 12px 32px #422b1510}
.orcamento-publico h1{font:700 36px/1.2 Georgia,serif;margin:8px 0 12px}
.orcamento-publico h2{font-size:24px;line-height:1.3;margin:0 0 16px}
.orcamento-publico p{margin:12px 0;color:#635b56}
.orcamento-publico .op-heading{display:flex;justify-content:space-between;gap:20px;align-items:flex-start}
.orcamento-publico .op-eyebrow{font-size:11px;font-weight:700;letter-spacing:2px;color:#8c5230;margin:0}
.orcamento-publico .op-status{background:#f5ebe2;color:#764325;border-radius:20px;padding:7px 14px;font-size:12px;font-weight:700;white-space:nowrap}
.orcamento-publico .op-id{font-size:12px;overflow-wrap:anywhere;margin:12px 0 28px}
.orcamento-publico .op-table{overflow-x:auto;border:1px solid #e9e1d8;border-radius:12px}
.orcamento-publico table{width:100%;border-collapse:collapse;min-width:650px;text-align:left;font-size:14px}
.orcamento-publico caption{text-align:left;padding:16px;font-weight:700}
.orcamento-publico th{background:#faf7f3;font-size:12px;color:#635b56}
.orcamento-publico th,.orcamento-publico td{padding:16px;border-top:1px solid #eee8e2}
.orcamento-publico .op-summary{display:flex;justify-content:space-between;gap:24px;padding:20px 0}
.orcamento-publico .op-summary p{font-size:13px}
.orcamento-publico .op-total{text-align:right;align-self:center}
.orcamento-publico .op-total span{display:block;font-size:13px;color:#635b56}
.orcamento-publico .op-total strong{font-size:32px;color:#8c5230}
.orcamento-publico .op-actions{border-top:1px solid #eee8e2;padding-top:16px}
.orcamento-publico .op-actions p{font-size:13px}
.orcamento-publico .op-actions>div,.orcamento-publico .op-dialog-actions{display:flex;gap:12px;justify-content:flex-end;flex-wrap:wrap}
.orcamento-publico button{font:600 14px system-ui,sans-serif;border:1px solid #8c5230;border-radius:9px;background:#8c5230;color:white;padding:13px 20px;cursor:pointer;min-height:46px}
.orcamento-publico button:hover{filter:brightness(.93)}
.orcamento-publico button:disabled{opacity:.6;cursor:wait}
.orcamento-publico .op-secondary{background:white;color:#764325;border-color:#d8c6b8}
.orcamento-publico :focus-visible{outline:3px solid #b66f40;outline-offset:3px}
.orcamento-publico .op-error{background:#fff1f0;color:#a12e25;padding:14px;border-radius:8px}
.orcamento-publico .op-company-error{max-width:1060px;margin:0 auto 20px}
.orcamento-publico .op-muted{color:#635b56;font-size:14px;margin:0}
.orcamento-publico .op-success,.orcamento-publico .op-closed{background:#edf6ef;color:#27633a;padding:14px;border-radius:8px}
.orcamento-publico footer{text-align:center;font-size:12px;color:#84786f;margin-top:24px}
.orcamento-publico dialog{border:1px solid #e9e1d8;border-radius:16px;padding:28px;max-width:510px;width:calc(100% - 32px);color:#292421;margin:auto}
.orcamento-publico dialog::backdrop{background:#241a13a6}
.orcamento-publico label{display:block;font-size:14px;font-weight:600;margin:20px 0 8px}
.orcamento-publico textarea{width:100%;font:inherit;padding:12px;border:1px solid #cbb9ab;border-radius:8px;resize:vertical}
.orcamento-publico small{color:#635b56}
.orcamento-publico .op-dialog-actions{margin-top:24px}
@media(max-width:650px){.orcamento-publico{padding:20px 12px}.orcamento-publico .op-card{padding:24px 18px}.orcamento-publico .op-heading{flex-direction:column}.orcamento-publico h1{font-size:30px}.orcamento-publico .op-brand{font-size:20px}.orcamento-publico .op-brand span{font-size:9px;letter-spacing:1px}.orcamento-publico .op-summary{flex-direction:column;gap:8px}.orcamento-publico .op-total{align-self:flex-start;text-align:left}.orcamento-publico .op-actions button{width:100%}}
.orcamento-publico .op-brand-left{display:flex;align-items:center;gap:12px}
.orcamento-publico .op-logo{height:40px;max-width:160px;object-fit:contain}
.orcamento-publico .op-brand-info{text-align:right;font-size:12px;color:#635b56;line-height:1.4}
.orcamento-publico .op-solicitacao{border:1px solid #e9e1d8;border-radius:12px;padding:20px;margin-top:20px;background:#faf7f3}
.orcamento-publico .op-solicitacao + .op-table{margin-top:24px}
.orcamento-publico .op-solicitacao h2{font:700 20px/1.3 Georgia,serif;margin:0 0 12px}
.orcamento-publico .op-solicitacao-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}
.orcamento-publico .op-solicitacao-grid>div{background:white;border:1px solid #e9e1d8;border-radius:10px;padding:12px 14px;display:flex;flex-direction:column;gap:4px}
.orcamento-publico .op-solicitacao-grid span{font-size:12px;color:#635b56}
.orcamento-publico .op-solicitacao-grid strong{font-size:14px}
.orcamento-publico .op-idiomas{grid-column:1/-1}
.orcamento-publico .op-descricao{margin-top:14px;background:white;border:1px solid #e9e1d8;border-radius:10px;padding:14px}
.orcamento-publico .op-descricao span{display:block;font-size:12px;color:#635b56;margin-bottom:6px}
.orcamento-publico .op-descricao p{margin:0;white-space:pre-wrap}
.orcamento-publico .op-arquivos{margin-top:14px;background:white;border:1px solid #e9e1d8;border-radius:10px;padding:14px}
.orcamento-publico .op-arquivos span{display:block;font-size:12px;color:#635b56;margin-bottom:8px}
.orcamento-publico .op-arquivos ul{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:8px}
.orcamento-publico .op-arquivos li{display:flex;align-items:center;justify-content:space-between;gap:12px;border:1px solid #eee8e2;border-radius:8px;padding:8px 12px}
.orcamento-publico .op-arquivos button{background:transparent;border:none;color:#8c5230;font:600 14px system-ui,sans-serif;padding:0;min-height:auto}
.orcamento-publico .op-arquivos button:disabled{opacity:.6}
.orcamento-publico .op-arquivos small{color:#84786f}
`;
