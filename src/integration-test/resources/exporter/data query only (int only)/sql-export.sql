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
WHERE
    [medical].[treatments].[id] = 1
GO
