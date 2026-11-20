-- Initial Auth schema baseline.


-- Dumped from database version 18.6 (Debian 18.6-1.pgdg13+2)
-- Dumped by pg_dump version 18.6 (Debian 18.6-1.pgdg13+2)




--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id uuid NOT NULL,
    ativo boolean NOT NULL,
    email character varying(255) NOT NULL,
    nome character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    senha character varying(255) NOT NULL,
    CONSTRAINT usuarios_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'ATENDENTE'::character varying, 'GESTOR_PROJETO'::character varying, 'FINANCEIRO'::character varying, 'RECURSO'::character varying])::text[])))
);


--
-- Name: usuarios ukkfsp0s1tflm1cwlj8idhqsad0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT ukkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'agent') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA public TO agent';
        EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO agent';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO agent';
    END IF;
END
$$;
