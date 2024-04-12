IF OBJECT_ID(N'tempdb..#cohort') IS NOT NULL
    DROP TABLE [#cohort]
GO

CREATE TABLE [#cohort] (
    [id] int NOT NULL)
GO

INSERT INTO [#cohort] (
    [id])
SELECT
    [people].[patients].[id]
FROM
    [people].[patients]
WHERE
    [people].[patients].[forename] = 'John'
GO

SELECT
    [people].[patients].[id],
    [people].[patients].[forename],
    [people].[patients].[surname],
    [people].[patients].[dateOfBirth],
    [people].[patients].[age],
    [people].[patients].[height],
    [people].[patients].[exercisesRegularly]
FROM
    [people].[patients]
JOIN
    [#cohort]
ON
    [#cohort].[id] = [people].[patients].[id]
GO

SELECT
    [medical].[episodes].[id],
    [medical].[episodes].[patientId],
    [medical].[episodes].[description],
    [medical].[episodes].[score],
    [medical].[episodes].[occurredAt],
    [medical].[episodes].[do_not_include]
FROM
    [medical].[episodes]
JOIN
    [#cohort]
ON
    [#cohort].[id] = [medical].[episodes].[patientId]
GO

SELECT
    [medical].[treatments].[id],
    [medical].[treatments].[patientId],
    [medical].[treatments].[episodeId],
    [medical].[treatments].[description],
    [medical].[treatments].[givenOn],
    [medical].[treatments].[do_not_include]
FROM
    [medical].[treatments]
JOIN
    [#cohort]
ON
    [#cohort].[id] = [medical].[treatments].[patientId]
GO

IF OBJECT_ID(N'tempdb..#cohort') IS NOT NULL
    DROP TABLE [#cohort]
GO